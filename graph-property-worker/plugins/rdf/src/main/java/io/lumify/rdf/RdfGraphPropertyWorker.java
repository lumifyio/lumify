package io.lumify.rdf;

import com.hp.hpl.jena.rdf.model.*;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.Property;
import org.securegraph.property.StreamingPropertyValue;

import java.io.*;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RdfGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RdfGraphPropertyWorker.class);
    public static final String RDF_TYPE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private String hasEntityIri;
    private String rdfConceptTypeIri;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        hasEntityIri = getConfiguration().get(Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY, null);
        checkNotNull(hasEntityIri, "configuration " + Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY + " is required");

        // rdfConceptTypeIri is not required because the concept type could have been set by some other means.
        rdfConceptTypeIri = getConfiguration().get(Configuration.ONTOLOGY_IRI_PREFIX + "rdf", null);
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        importRdf(getGraph(), in, null, data, data.getVisibility(), getAuthorizations());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = LumifyProperties.MIME_TYPE.getPropertyValue(element);
        if (!RdfOntology.MIME_TYPE_TEXT_RDF.equals(mimeType)) {
            return false;
        }

        if (!LumifyProperties.RAW.getPropertyName().equals(property.getName())) {
            return false;
        }

        return true;
    }

    public void importRdf(Graph graph, File inputFile, GraphPropertyWorkData data, Visibility visibility, Authorizations authorizations) throws IOException {
        InputStream in = new FileInputStream(inputFile);
        try {
            File baseDir = inputFile.getParentFile();
            importRdf(graph, in, baseDir, data, visibility, authorizations);
        } finally {
            in.close();
        }
    }

    public void importRdf(Graph graph, InputStream in, File baseDir, GraphPropertyWorkData data, Visibility visibility, Authorizations authorizations) {
        if (rdfConceptTypeIri != null && data != null) {
            LumifyProperties.CONCEPT_TYPE.setProperty(data.getElement(), rdfConceptTypeIri, data.createPropertyMetadata(), visibility, getAuthorizations());
        }

        Model model = ModelFactory.createDefaultModel();
        model.read(in, null);
        importRdfModel(graph, model, baseDir, data, visibility, authorizations);
    }

    public void importRdfModel(Graph graph, Model model, File baseDir, GraphPropertyWorkData data, Visibility visibility, Authorizations authorizations) {
        ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {
            Resource subject = subjects.next();
            importSubject(graph, subject, baseDir, data, visibility, authorizations);
        }
    }

    public void importSubject(Graph graph, Resource subject, File baseDir, GraphPropertyWorkData data, Visibility visibility, Authorizations authorizations) {
        LOGGER.info("importSubject: %s", subject.toString());
        String graphVertexId = getGraphVertexId(subject);
        VertexBuilder vertexBuilder = graph.prepareVertex(graphVertexId, visibility);
        if (data != null) {
            data.setVisibilityJsonOnElement(vertexBuilder);
        }

        StmtIterator statements = subject.listProperties();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            RDFNode obj = statement.getObject();
            if (obj instanceof Resource) {
                if (isConceptTypeResource(statement)) {
                    String value = statement.getResource().toString();
                    Map<String, Object> metadata = null;
                    if (data != null) {
                        metadata = data.createPropertyMetadata();
                    }
                    LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, value, metadata, visibility);
                }
            } else if (obj instanceof Literal) {
                LOGGER.info("set property on %s to %s", subject.toString(), statement.toString());
                importLiteral(vertexBuilder, statement, baseDir, data, visibility);
            } else {
                throw new LumifyException("Unhandled object type: " + obj.getClass().getName());
            }
        }

        Vertex v = vertexBuilder.save(authorizations);

        if (data != null) {
            String edgeId = data.getElement().getId() + "_hasEntity_" + v.getId();
            EdgeBuilder e = graph.prepareEdge(edgeId, (Vertex) data.getElement(), v, hasEntityIri, visibility);
            data.setVisibilityJsonOnElement(e);
            e.save(authorizations);

            addVertexToWorkspaceIfNeeded(data, v);
        }

        statements = subject.listProperties();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            RDFNode obj = statement.getObject();
            if (obj instanceof Resource) {
                if (isConceptTypeResource(statement)) {
                    continue;
                }
                importResource(graph, v, statement, data, visibility, authorizations);
            }
        }
    }

    private boolean isConceptTypeResource(Statement statement) {
        String label = statement.getPredicate().toString();
        return label.equals(RDF_TYPE_URI);
    }

    private void importLiteral(VertexBuilder v, Statement statement, File baseDir, GraphPropertyWorkData data, Visibility visibility) {
        String propertyName = statement.getPredicate().toString();
        String valueString = statement.getLiteral().toString();
        Object value = valueString;
        String propertyKey = RdfGraphPropertyWorker.class.getName() + "_" + hashValue(valueString);

        if (valueString.startsWith("streamingValue:")) {
            value = convertStreamingValueJsonToValueObject(baseDir, valueString);
        }

        Map<String, Object> metadata = null;
        if (data != null) {
            metadata = data.createPropertyMetadata();
        }
        v.addPropertyValue(propertyKey, propertyName, value, metadata, visibility);
    }

    private String hashValue(String valueString) {
        // we need a unique value but it's a bit silly to store a whole md5 hash
        return DigestUtils.md5Hex(valueString).substring(0, 10);
    }

    private Object convertStreamingValueJsonToValueObject(File baseDir, String valueString) {
        JSONObject streamingValueJson = new JSONObject(valueString.substring("streamingValue:".length()));
        String fileName = streamingValueJson.getString("fileName");
        if (baseDir == null) {
            throw new LumifyException("Could not import streamingValue. No baseDir specified.");
        }
        File file = new File(baseDir, fileName);
        InputStream in;
        try {
            if (!file.exists()) {
                throw new LumifyException("File " + file.getAbsolutePath() + " does not exist.");
            }
            in = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            throw new LumifyException("File " + file.getAbsolutePath() + " does not exist.");
        }
        StreamingPropertyValue spv = new StreamingPropertyValue(in, byte[].class);
        spv.searchIndex(false);
        spv.store(true);
        return spv;
    }

    private void importResource(Graph graph, Vertex outVertex, Statement statement, GraphPropertyWorkData data, Visibility visibility, Authorizations authorizations) {
        String label = statement.getPredicate().toString();
        String vertexId = getGraphVertexId(statement.getResource());
        VertexBuilder inVertexBuilder = graph.prepareVertex(vertexId, visibility);
        if (data != null) {
            data.setVisibilityJsonOnElement(inVertexBuilder);
        }
        Vertex inVertex = inVertexBuilder.save(authorizations);
        if (data != null) {
            addVertexToWorkspaceIfNeeded(data, inVertex);
        }
        String edgeId = outVertex.getId() + "_" + label + "_" + inVertex.getId();

        EdgeBuilder e = graph.prepareEdge(edgeId, outVertex, inVertex, label, visibility);
        if (data != null) {
            data.setVisibilityJsonOnElement(e);
        }
        e.save(authorizations);
        LOGGER.info("importResource: %s = %s", label, vertexId);
    }

    private String getGraphVertexId(Resource subject) {
        String subjectUri = subject.getURI();
        checkNotNull(subjectUri, "could not get uri of subject: " + subject);
        int lastPound = subjectUri.lastIndexOf('#');
        checkArgument(lastPound >= 1, "Could not find '#' in subject uri: " + subjectUri);
        return subjectUri.substring(lastPound + 1);
    }
}
