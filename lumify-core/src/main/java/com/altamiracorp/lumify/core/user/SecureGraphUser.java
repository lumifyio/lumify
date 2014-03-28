package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserStatus;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.securegraph.Vertex;

import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.CURRENT_WORKSPACE;
import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.USERNAME;

public class SecureGraphUser implements User {
    private static final long serialVersionUID = 1L;
    private Vertex user;
    private ModelUserContext modelUserContext;

    public SecureGraphUser(Vertex user, ModelUserContext modelUserContext) {
        this.user = user;
        this.modelUserContext = modelUserContext;
    }

    public String getUserId() {
        return user.getId().toString();
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public String getUserName() {
        return USERNAME.getPropertyValue(user);
    }

    public String getCurrentWorkspace() {
        return CURRENT_WORKSPACE.getPropertyValue(user);
    }

    public UserType getUserType() {
        return UserType.USER;
    }

    public Vertex getUser() {
        return user;
    }

    @Override
    public UserStatus getUserStatus() {
        return null;
    }

    @Override
    public void setCurrentWorkspace(String currentWorkspace) {
        CURRENT_WORKSPACE.setProperty(user, currentWorkspace, user.getVisibility());
    }
}
