package io.lumify.javaCodeIngest;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.ontology.OntologyLumifyProperties;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.RawLumifyProperties;
import org.securegraph.*;
import org.securegraph.property.StreamingPropertyValue;

import java.io.InputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.securegraph.util.IterableUtils.toList;

public class JarFileGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        OntologyLumifyProperties.CONCEPT_TYPE.setProperty(data.getElement(), Ontology.CONCEPT_TYPE_JAR_FILE, data.getProperty().getVisibility(), getAuthorizations());
        RawLumifyProperties.MIME_TYPE.setProperty(data.getElement(), "application/java-archive", data.getProperty().getVisibility(), getAuthorizations());

        List<Vertex> existingFileVerticies = toList(((Vertex) data.getElement()).getVertices(Direction.BOTH, Ontology.EDGE_LABEL_JAR_CONTAINS, getAuthorizations()));

        JarInputStream jarInputStream = new JarInputStream(in);
        JarEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (jarEntry.isDirectory()) {
                continue;
            }

            if (fileAlreadyExists(existingFileVerticies, jarEntry.getName())) {
                continue;
            }

            StreamingPropertyValue rawValue = new StreamingPropertyValue(jarInputStream, byte[].class);
            rawValue.searchIndex(false);

            Vertex jarEntryVertex = createFileVertex(jarEntry, rawValue, data);

            createJarContainsFileEdge(jarEntryVertex, data);

            getGraph().flush();

            getWorkQueueRepository().pushGraphPropertyQueue(jarEntryVertex, RawLumifyProperties.RAW.getProperty(jarEntryVertex));
        }
    }

    private boolean fileAlreadyExists(List<Vertex> existingFileVerticies, String fileName) {
        for (Vertex v : existingFileVerticies) {
            String existingFileName = RawLumifyProperties.FILE_NAME.getPropertyValue(v);
            if (existingFileName == null) {
                return false;
            }
            if (existingFileName.equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    private void createJarContainsFileEdge(Vertex jarEntryVertex, GraphPropertyWorkData data) {
        EdgeBuilder jarContainsEdgeBuilder = getGraph().prepareEdge((Vertex) data.getElement(), jarEntryVertex, Ontology.EDGE_LABEL_JAR_CONTAINS, data.getProperty().getVisibility());
        jarContainsEdgeBuilder.save(getAuthorizations());
    }

    private Vertex createFileVertex(JarEntry jarEntry, StreamingPropertyValue rawValue, GraphPropertyWorkData data) {
        VertexBuilder jarEntryVertexBuilder = getGraph().prepareVertex(data.getProperty().getVisibility());
        LumifyProperties.TITLE.setProperty(jarEntryVertexBuilder, jarEntry.getName(), data.getProperty().getVisibility());
        OntologyLumifyProperties.CONCEPT_TYPE.setProperty(jarEntryVertexBuilder, Ontology.CONCEPT_TYPE_CLASS_FILE, data.getProperty().getVisibility());
        RawLumifyProperties.MIME_TYPE.setProperty(jarEntryVertexBuilder, "application/octet-stream", data.getProperty().getVisibility());
        RawLumifyProperties.FILE_NAME.setProperty(jarEntryVertexBuilder, jarEntry.getName(), data.getProperty().getVisibility());
        RawLumifyProperties.RAW.setProperty(jarEntryVertexBuilder, rawValue, data.getProperty().getVisibility());
        return jarEntryVertexBuilder.save(getAuthorizations());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(RawLumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String fileName = RawLumifyProperties.FILE_NAME.getPropertyValue(element);
        if (fileName == null || !fileName.endsWith(".jar")) {
            return false;
        }

        return true;
    }
}
