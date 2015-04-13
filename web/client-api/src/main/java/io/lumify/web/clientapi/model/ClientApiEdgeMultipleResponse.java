package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiEdgeMultipleResponse implements ClientApiObject {
    private List<ClientApiEdgeWithVertexData> edges = new ArrayList<ClientApiEdgeWithVertexData>();

    public List<ClientApiEdgeWithVertexData> getEdges() {
        return edges;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
