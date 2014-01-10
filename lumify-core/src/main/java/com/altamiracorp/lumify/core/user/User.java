package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.securegraph.Authorizations;

public class User {
    private static final Authorizations AUTHORIZATIONS = new Authorizations();
    private String username;
    private String rowKey;
    private String currentWorkspace;
    private ModelUserContext modelUserContext;
    private String userType;

    public User(String rowKey, String username, String currentWorkspace, ModelUserContext modelUserContext, String userType) {
        this.rowKey = rowKey;
        this.username = username;
        this.currentWorkspace = currentWorkspace;
        this.modelUserContext = modelUserContext;
        this.userType = userType;
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

    public String getUserType() {
        return userType;
    }

    public Authorizations getAuthorizations() {
        return AUTHORIZATIONS;
    }
}
