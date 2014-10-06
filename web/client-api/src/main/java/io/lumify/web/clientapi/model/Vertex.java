package io.lumify.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class Vertex extends Element {
    private List<String> edgeLabels = new ArrayList<String>();

    public List<String> getEdgeLabels() {
        return edgeLabels;
    }

    @Override
    public String getType() {
        return "vertex";
    }
}
