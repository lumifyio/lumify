package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiVertexMultipleResponse implements ClientApiObject {
    private boolean requiredFallback;
    private List<ClientApiVertex> vertices = new ArrayList<ClientApiVertex>();

    public boolean isRequiredFallback() {
        return requiredFallback;
    }

    public void setRequiredFallback(boolean requiredFallback) {
        this.requiredFallback = requiredFallback;
    }

    public List<ClientApiVertex> getVertices() {
        return vertices;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
