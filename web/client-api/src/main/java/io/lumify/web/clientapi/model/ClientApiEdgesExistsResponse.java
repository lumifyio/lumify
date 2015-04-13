package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.HashMap;
import java.util.Map;

public class ClientApiEdgesExistsResponse implements ClientApiObject {
    private Map<String, Boolean> exists = new HashMap<>();

    public Map<String, Boolean> getExists() {
        return exists;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
