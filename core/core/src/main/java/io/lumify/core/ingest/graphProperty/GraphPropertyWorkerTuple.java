package io.lumify.core.ingest.graphProperty;

import org.json.JSONObject;

public class GraphPropertyWorkerTuple {
    private final Object messageId;
    private final JSONObject json;

    public GraphPropertyWorkerTuple(Object messageId, JSONObject json) {
        this.messageId = messageId;
        this.json = json;
    }

    public Object getMessageId() {
        return messageId;
    }

    public JSONObject getJson() {
        return json;
    }

    @Override
    public String toString() {
        return "GraphPropertyWorkerTuple{" +
                "messageId=" + messageId +
                ", json=" + json +
                '}';
    }
}
