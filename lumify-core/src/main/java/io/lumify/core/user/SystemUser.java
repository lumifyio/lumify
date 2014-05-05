package io.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserStatus;
import io.lumify.core.model.user.UserType;

public class SystemUser implements User {
    private static final long serialVersionUID = 1L;
    private static final String SYSTEM_USERNAME = "system";
    private final ModelUserContext modelUserContext;

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
    public String getCurrentWorkspaceId() {
        return null;
    }

    @Override
    public String toString() {
        return "SystemUser";
    }
}
