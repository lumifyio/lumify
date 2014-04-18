package com.altamiracorp.lumify.securegraph.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.lumify.core.user.User;

import java.io.Serializable;

public class SecureGraphUser implements User, Serializable {
    private static final long serialVersionUID = 6688073934273514248L;
    private ModelUserContext modelUserContext;
    private String displayName;
    private String userId;

    // required for Serializable
    protected SecureGraphUser() {

    }

    public SecureGraphUser(String userId, String displayName, ModelUserContext modelUserContext) {
        this.displayName = displayName;
        this.modelUserContext = modelUserContext;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public String getUserStatus() {
        return null;
    }

    @Override
    public String toString() {
        return "SecureGraphUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "'}";
    }
}
