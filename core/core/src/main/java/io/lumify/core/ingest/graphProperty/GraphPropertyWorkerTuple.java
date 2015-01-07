package io.lumify.core.ingest.graphProperty;

import io.lumify.core.ingest.WorkerTuple;
import org.json.JSONObject;

public class GraphPropertyWorkerTuple extends WorkerTuple{
    public GraphPropertyWorkerTuple(Object messageId, JSONObject json) {
        super(messageId, json);
    }

    @Override
    public String toString() {
        return "GraphPropertyWorkerTuple{" +
                "messageId=" + getMessageId() +
                ", json=" + getJson() +
                '}';
    }
}
