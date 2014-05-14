package io.lumify.securegraph.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserType;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;

import java.io.Serializable;
import java.util.Set;

public class SecureGraphUser implements User, Serializable {
    private static final long serialVersionUID = 6688073934273514248L;
    private ModelUserContext modelUserContext;
    private String displayName;
    private String userId;
    private String userName;
    private String userStatus;
    private Set<Privilege> privileges;
    private String currentWorkspaceId;

    // required for Serializable
    protected SecureGraphUser() {

    }

    public SecureGraphUser(String userId, String userName, String displayName, ModelUserContext modelUserContext, String userStatus, Set<Privilege> privileges, String currentWorkspaceId) {
        this.displayName = displayName;
        this.userName = userName;
        this.modelUserContext = modelUserContext;
        this.userId = userId;
        this.userStatus = userStatus;
        this.privileges = privileges;
        this.currentWorkspaceId = currentWorkspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public String getUsername() {
        return userName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public String getUserStatus() {
        return userStatus;
    }

    @Override
    public String getCurrentWorkspaceId() {
        return currentWorkspaceId;
    }

    public Set<Privilege> getPrivileges() {
        return this.privileges;
    }

    public void setUserStatus(String status) {
        this.userStatus = status;
    }

    @Override
    public String toString() {
        return "SecureGraphUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "', privileges=" + getPrivileges() + "}";
    }
}
