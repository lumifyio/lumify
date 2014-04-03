package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.securegraph.Vertex;
import org.json.JSONObject;

public interface Workspace {
    JSONObject toJson(boolean includeVertices);

    String getId();

    String getCreatorUserId();

    String getTitle();

    Vertex getVertex();

    boolean hasWritePermissions(String userId);
}

