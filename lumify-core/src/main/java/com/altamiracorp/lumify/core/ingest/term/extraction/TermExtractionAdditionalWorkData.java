package com.altamiracorp.lumify.core.ingest.term.extraction;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;

public class TermExtractionAdditionalWorkData {

    private GraphVertex graphVertex;

    public void setGraphVertex(GraphVertex graphVertex) {
        this.graphVertex = graphVertex;
    }

    public GraphVertex getGraphVertex() {
        return graphVertex;
    }
}
