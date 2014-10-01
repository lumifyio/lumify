package io.lumify.rdf;

import com.hp.hpl.jena.rdf.model.*;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RdfImport {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RdfImport.class);
    public static final String RDF_TYPE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public void importRdf(Graph graph, File inputFile, Authorizations authorizations) throws IOException {
        InputStream in = new FileInputStream(inputFile);
        try {
            importRdf(graph, in, authorizations);
        } finally {
            in.close();
        }
    }

    public void importRdf(Graph graph, InputStream in, Authorizations authorizations) {
        Model model = ModelFactory.createDefaultModel();
        model.read(in, null);
        importRdfModel(graph, model, authorizations);
    }

    public void importRdfModel(Graph graph, Model model, Authorizations authorizations) {
        ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {
            Resource subject = subjects.next();
            importSubject(graph, subject, authorizations);
        }
    }

    public void importSubject(Graph graph, Resource subject, Authorizations authorizations) {
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
                importLiteral(vertexBuilder, statement, visibility);
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

    private void importLiteral(VertexBuilder v, Statement statement, Visibility visibility) {
        String propertyName = statement.getPredicate().toString();
        String value = statement.getLiteral().toString();
        v.setProperty(propertyName, value, visibility);
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
