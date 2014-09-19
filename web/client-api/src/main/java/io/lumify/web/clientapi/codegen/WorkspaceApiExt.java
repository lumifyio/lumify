package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.model.WorkspaceUpdateData;

public class WorkspaceApiExt extends WorkspaceApi {
    public void update(WorkspaceUpdateData updateData) throws ApiException {
        update(ApiInvoker.serialize(updateData));
    }
}
