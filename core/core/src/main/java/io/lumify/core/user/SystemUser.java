package io.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.web.clientapi.model.Privilege;
import io.lumify.web.clientapi.model.UserStatus;
import io.lumify.web.clientapi.model.UserType;
import org.json.JSONObject;

import java.util.Date;
import java.util.Set;

public class SystemUser implements User {
    private static final long serialVersionUID = 1L;
    private static final String SYSTEM_USERNAME = "system";
    private final ModelUserContext modelUserContext;

    public SystemUser(ModelUserContext modelUserContext) {
        this.modelUserContext = modelUserContext;
    }

    @Override
    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    @Override
    public String getUserId() {
        return "";
    }

    @Override
    public String getUsername() {
        return SYSTEM_USERNAME;
    }

    @Override
    public String getDisplayName() {
        return SYSTEM_USERNAME;
    }

    @Override
    public String getEmailAddress() {
        return SYSTEM_USERNAME;
    }

    @Override
    public Date getCreateDate() {
        return new Date(0);
    }

    @Override
    public Date getCurrentLoginDate() {
        return null;
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        return null;
    }

    @Override
    public Date getPreviousLoginDate() {
        return null;
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        return null;
    }

    @Override
    public int getLoginCount() {
        return 0;
    }

    @Override
    public UserType getUserType() {
        return UserType.SYSTEM;
    }

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.OFFLINE;
    }

    @Override
    public String getCurrentWorkspaceId() {
        return null;
    }

    @Override
    public JSONObject getUiPreferences() {
        return new JSONObject();
    }

    @Override
    public Set<Privilege> getPrivileges() {
        return null;
    }

    @Override
    public String toString() {
        return "SystemUser";
    }

    @Override
    public String getPasswordResetToken() {
        return null;
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        return null;
    }
}
