package io.lumify.core.ingest;

import org.json.JSONObject;

public class WorkerTuple {
    private final Object messageId;
    private final JSONObject json;

    public WorkerTuple(Object messageId, JSONObject json) {
        this.messageId = messageId;
        this.json = json;
    }

    public Object getMessageId() {
        return messageId;
    }

    public JSONObject getJson() {
        return json;
    }
}
