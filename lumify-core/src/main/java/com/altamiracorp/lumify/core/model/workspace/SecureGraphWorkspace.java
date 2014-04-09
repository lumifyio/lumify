package com.altamiracorp.lumify.core.model.workspace;

public class SecureGraphWorkspace implements Workspace {
    private String displayTitle;
    private String workspaceId;

    public SecureGraphWorkspace(String displayTitle, String workspaceId) {
        this.displayTitle = displayTitle;
        this.workspaceId = workspaceId;
    }


    @Override
    public String getId() {
        return workspaceId;
    }

    @Override
    public String getDisplayTitle() {
        return displayTitle;
    }
}