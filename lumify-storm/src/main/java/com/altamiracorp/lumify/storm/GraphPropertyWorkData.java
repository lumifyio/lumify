package com.altamiracorp.lumify.storm;

import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;

public class GraphPropertyWorkData {
    private final Vertex vertex;
    private final Property property;

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
}
