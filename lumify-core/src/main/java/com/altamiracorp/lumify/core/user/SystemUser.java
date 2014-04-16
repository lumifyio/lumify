package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserStatus;
import com.altamiracorp.lumify.core.model.user.UserType;

public class SystemUser implements User {
    private static final long serialVersionUID = 1L;
    private static final String SYSTEM_USERNAME = "system";
    private ModelUserContext modelUserContext;

    public SystemUser(ModelUserContext modelUserContext) {
        this.modelUserContext = modelUserContext;
    }

    public String getUserId() {
        return "";
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public String getDisplayName() {
        return SYSTEM_USERNAME;
    }

    public UserType getUserType() {
        return UserType.SYSTEM;
    }

    @Override
    public String getUserStatus() {
        return UserStatus.OFFLINE.name();
    }

    @Override
    public String toString() {
        return "SystemUser";
    }
}
