package io.lumify.core.model.workspace;

import io.lumify.web.clientapi.model.WorkspaceAccess;

public class WorkspaceUser {
    private final String userId;
    private final WorkspaceAccess workspaceAccess;
    private boolean isCreator = false;

    public WorkspaceUser(String userId, WorkspaceAccess workspaceAccess, boolean isCreator) {
        this.userId = userId;
        this.workspaceAccess = workspaceAccess;
        this.isCreator = isCreator;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isCreator() {
        return isCreator;
    }

    public WorkspaceAccess getWorkspaceAccess() {
        return workspaceAccess;
    }
}
