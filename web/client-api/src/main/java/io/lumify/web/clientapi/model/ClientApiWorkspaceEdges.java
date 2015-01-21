package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspaceEdges implements ClientApiObject {
    private List<ClientApiEdge> edges = new ArrayList<ClientApiEdge>();

    public List<ClientApiEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<ClientApiEdge> edges) {
        this.edges = edges;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
