package io.lumify.securegraph.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserType;
import io.lumify.core.user.Roles;
import io.lumify.core.user.User;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

public class SecureGraphUser implements User, Serializable {
    private static final long serialVersionUID = 6688073934273514248L;
    private ModelUserContext modelUserContext;
    private String displayName;
    private String userId;
    private String userStatus;
    private int roles;

    // required for Serializable
    protected SecureGraphUser() {

    }

    public SecureGraphUser(String userId, String displayName, ModelUserContext modelUserContext, String userStatus, Collection<Roles> roles) {
        this.displayName = displayName;
        this.modelUserContext = modelUserContext;
        this.userId = userId;
        this.userStatus = userStatus;
        this.roles = Roles.toBits(roles);
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
        return userStatus;
    }

    public Set<Roles> getRoles() {
        return Roles.toSet(this.roles);
    }

    public void setUserStatus(String status) {
        this.userStatus = status;
    }

    @Override
    public String toString() {
        return "SecureGraphUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "', roles=" + getRoles() + "}";
    }
}
