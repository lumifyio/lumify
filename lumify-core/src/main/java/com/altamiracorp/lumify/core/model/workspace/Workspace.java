package com.altamiracorp.lumify.core.model.workspace;

import org.json.JSONObject;

import java.io.Serializable;

public interface Workspace extends Serializable{

    JSONObject toJson(boolean includeVertices);

    String getId();

    String getCreatorUserId();

    String getDisplayTitle();

    boolean hasWritePermissions(String userId);
}

