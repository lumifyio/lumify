package com.altamiracorp.lumify.core.model.workspace;

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
