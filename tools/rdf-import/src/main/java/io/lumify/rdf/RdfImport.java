package io.lumify.rdf;

import com.hp.hpl.jena.rdf.model.*;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.property.StreamingPropertyValue;

import java.io.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RdfImport {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RdfImport.class);
    public static final String RDF_TYPE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public void importRdf(Graph graph, File inputFile, Authorizations authorizations) throws IOException {
        InputStream in = new FileInputStream(inputFile);
        try {
            File baseDir = inputFile.getParentFile();
            importRdf(graph, in, baseDir, authorizations);
        } finally {
            in.close();
        }
    }

    public void importRdf(Graph graph, InputStream in, File baseDir, Authorizations authorizations) {
        Model model = ModelFactory.createDefaultModel();
        model.read(in, null);
        importRdfModel(graph, model, baseDir, authorizations);
    }

    public void importRdfModel(Graph graph, Model model, File baseDir, Authorizations authorizations) {
        ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {
            Resource subject = subjects.next();
            importSubject(graph, subject, baseDir, authorizations);
        }
    }

    public void importSubject(Graph graph, Resource subject, File baseDir, Authorizations authorizations) {
        LOGGER.info("importSubject: %s", subject.toString());
        String graphVertexId = getGraphVertexId(subject);
        Visibility visibility = getVisibility(subject);
        VertexBuilder vertexBuilder = graph.prepareVertex(graphVertexId, visibility);

        StmtIterator statements = subject.listProperties();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            RDFNode obj = statement.getObject();
            if (obj instanceof Resource) {
                if (isConceptTypeResource(statement)) {
                    String value = statement.getResource().toString();
                    LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, value, visibility);
                }
            } else if (obj instanceof Literal) {
                LOGGER.info("set property on %s to %s", subject.toString(), statement.toString());
                importLiteral(vertexBuilder, statement, baseDir, visibility);
            } else {
                throw new LumifyException("Unhandled object type: " + obj.getClass().getName());
            }
        }

        Vertex v = vertexBuilder.save(authorizations);

        statements = subject.listProperties();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            RDFNode obj = statement.getObject();
            if (obj instanceof Resource) {
                if (isConceptTypeResource(statement)) {
                    continue;
                }
                importResource(graph, v, statement, visibility, authorizations);
            }
        }
    }

    private boolean isConceptTypeResource(Statement statement) {
        String label = statement.getPredicate().toString();
        return label.equals(RDF_TYPE_URI);
    }

    private void importLiteral(VertexBuilder v, Statement statement, File baseDir, Visibility visibility) {
        String propertyName = statement.getPredicate().toString();
        String valueString = statement.getLiteral().toString();
        Object value = valueString;
        String propertyKey = RdfImport.class.getName() + "_" + hashValue(valueString);

        if (valueString.startsWith("streamingValue:")) {
            value = convertStreamingValueJsonToValueObject(baseDir, valueString);
        }

        v.addPropertyValue(propertyKey, propertyName, value, visibility);
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

    private void importResource(Graph graph, Vertex outVertex, Statement statement, Visibility visibility, Authorizations authorizations) {
        String label = statement.getPredicate().toString();
        String vertexId = getGraphVertexId(statement.getResource());
        Vertex inVertex = graph.addVertex(vertexId, visibility, authorizations);
        String edgeId = outVertex.getId() + "_" + label + "_" + inVertex.getId();

        graph.addEdge(edgeId, outVertex, inVertex, label, visibility, authorizations);
        LOGGER.info("importResource: %s = %s", label, vertexId);
    }

    private Visibility getVisibility(Resource subject) {
        return new Visibility("");
    }

    private String getGraphVertexId(Resource subject) {
        String subjectUri = subject.getURI();
        checkNotNull(subjectUri, "could not get uri of subject: " + subject);
        int lastPound = subjectUri.lastIndexOf('#');
        checkArgument(lastPound >= 1, "Could not find '#' in subject uri: " + subjectUri);
        return subjectUri.substring(lastPound + 1);
    }
}
