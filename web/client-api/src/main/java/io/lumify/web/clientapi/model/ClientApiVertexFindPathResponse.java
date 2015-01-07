package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiVertexFindPathResponse implements ClientApiObject {
    private List<List<ClientApiVertex>> paths = new ArrayList<List<ClientApiVertex>>();

    public List<List<ClientApiVertex>> getPaths() {
        return paths;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
