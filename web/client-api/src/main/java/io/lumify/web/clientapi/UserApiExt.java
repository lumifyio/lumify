package io.lumify.web.clientapi;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.ClientApiUsers;

public class UserApiExt extends io.lumify.web.clientapi.codegen.UserApi {
    public ClientApiUsers getAll() throws ApiException {
        return getAll(null, null);
    }

    public ClientApiUsers getAll(String query) throws ApiException {
        return getAll(query, null);
    }

    public ClientApiUsers getAllForWorkspace(String workspaceId) throws ApiException {
        return getAll(null, workspaceId);
    }

    public ClientApiUsers getAllForWorkspace(String query, String workspaceId) throws ApiException {
        return getAll(query, workspaceId);
    }
}
