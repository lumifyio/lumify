package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.LumifyClientApiException;
import org.json.JSONObject;

public abstract class WorkspaceDiffItem {
    private final JSONObject diffJson;

    public WorkspaceDiffItem(JSONObject diffJson) {
        this.diffJson = diffJson;
    }

    public static WorkspaceDiffItem create(JSONObject diffJson) {
        String type = diffJson.getString("type");
        if ("VertexDiffItem".equals(type)) {
            return new VertexDiffItem(diffJson);
        } else if ("PropertyDiffItem".equals(type)) {
            return new PropertyDiffItem(diffJson);
        } else if ("EdgeDiffItem".equals(type)) {
            return new EdgeDiffItem(diffJson);
        } else {
            throw new LumifyClientApiException("Unhandled workspace diff item type: " + type);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "diffJson=" + diffJson +
                '}';
    }

    protected JSONObject getDiffJson() {
        return diffJson;
    }
}
