package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;

public class User {
    private String username;
    private String rowKey;
    private String currentWorkspace;
    private ModelUserContext modelUserContext;

    public User(String rowKey, String username, String currentWorkspace, ModelUserContext modelUserContext) {
        this.rowKey = rowKey;
        this.username = username;
        this.currentWorkspace = currentWorkspace;
        this.modelUserContext = modelUserContext;
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public String getUsername() {
        return username;
    }

    public String getRowKey() {
        return rowKey;
    }

    public String getCurrentWorkspace() {
        return currentWorkspace;
    }

    public void setCurrentWorkspace(String currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
    }
}
