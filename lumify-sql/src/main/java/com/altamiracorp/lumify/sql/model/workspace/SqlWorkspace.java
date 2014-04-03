package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.securegraph.Vertex;
import org.json.JSONObject;

public class SqlWorkspace implements Workspace {
    @Override
    public JSONObject toJson(boolean includeVertices) {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getCreatorUserId() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public Vertex getVertex() {
        return null;
    }

    @Override
    public boolean hasWritePermissions(String userId) {
        return false;
    }
}
