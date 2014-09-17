package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class PublishFailure {
    private final JSONObject failureJson;

    public PublishFailure(JSONObject failureJson) {
        this.failureJson = failureJson;
    }

    @Override
    public String toString() {
        return "PublishFailure{" +
                "failureJson=" + failureJson +
                '}';
    }
}
