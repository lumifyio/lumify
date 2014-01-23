package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.securegraph.Authorizations;

import java.io.Serializable;

public class User implements Serializable {
    private final Authorizations authorizations;
    private String username;
    private String rowKey;
    private String currentWorkspace;
    private ModelUserContext modelUserContext;
    private String userType;

    public User(String rowKey, String username, String currentWorkspace, ModelUserContext modelUserContext, String userType, Authorizations authorizations) {
        this.rowKey = rowKey;
        this.username = username;
        this.currentWorkspace = currentWorkspace;
        this.modelUserContext = modelUserContext;
        this.userType = userType;
        this.authorizations = authorizations;
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
        return authorizations;
    }
}
