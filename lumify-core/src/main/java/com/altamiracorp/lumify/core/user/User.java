package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserStatus;
import com.altamiracorp.lumify.core.model.user.UserType;

import java.io.Serializable;

public interface User extends Serializable {
    public String getUserId();

    public ModelUserContext getModelUserContext();

    public String getDisplayName();

    public String getCurrentWorkspace();

    public UserType getUserType();

    public UserStatus getUserStatus ();

    public void setCurrentWorkspace (String currentWorkspace);
}
