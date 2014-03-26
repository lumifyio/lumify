package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;

import java.io.File;

public class GraphPropertyWorkData {
    private final Vertex vertex;
    private final Property property;
    private File localFile;

    public GraphPropertyWorkData(Vertex vertex, Property property) {
        this.vertex = vertex;
        this.property = property;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public Property getProperty() {
        return property;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public File getLocalFile() {
        return localFile;
    }
}
