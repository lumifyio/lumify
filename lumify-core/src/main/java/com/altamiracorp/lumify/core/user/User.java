package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.lumify.core.model.workspace.Workspace;

import java.io.Serializable;

public interface User extends Serializable {
    public String getUserId();

    public ModelUserContext getModelUserContext();

    public String getDisplayName();

    public Workspace getCurrentWorkspace();

    public UserType getUserType();

    public String getUserStatus();

    public void setCurrentWorkspace(Workspace currentWorkspace);
}
