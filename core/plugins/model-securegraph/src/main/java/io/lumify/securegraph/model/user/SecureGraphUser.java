package io.lumify.securegraph.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserType;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class SecureGraphUser implements User, Serializable {
    private static final long serialVersionUID = 6688073934273514248L;
    private ModelUserContext modelUserContext;
    private String userId;
    private String username;
    private String displayName;
    private String emailAddress;
    private Date createDate;
    private Date currentLoginDate;
    private String currentLoginRemoteAddr;
    private Date previousLoginDate;
    private String previousLoginRemoteAddr;
    private int loginCount;
    private String userStatus;
    private Set<Privilege> privileges;
    private String currentWorkspaceId;
    private JSONObject preferences;

    // required for Serializable
    protected SecureGraphUser() {

    }

    public SecureGraphUser(String userId, String username, String displayName, String emailAddress, Date createDate, Date currentLoginDate, String currentLoginRemoteAddr, Date previousLoginDate, String previousLoginRemoteAddr, int loginCount, ModelUserContext modelUserContext, String userStatus, Set<Privilege> privileges, String currentWorkspaceId, JSONObject preferences) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.createDate = createDate;
        this.currentLoginDate = currentLoginDate;
        this.currentLoginRemoteAddr = currentLoginRemoteAddr;
        this.previousLoginDate = previousLoginDate;
        this.previousLoginRemoteAddr = previousLoginRemoteAddr;
        this.loginCount = loginCount;
        this.modelUserContext = modelUserContext;
        this.userStatus = userStatus;
        this.privileges = privileges;
        this.currentWorkspaceId = currentWorkspaceId;
        this.preferences = preferences;
    }

    @Override
    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getEmailAddress() { return emailAddress; }

    @Override
    public Date getCreateDate() { return createDate; }

    @Override
    public Date getCurrentLoginDate() { return currentLoginDate; }

    @Override
    public String getCurrentLoginRemoteAddr() { return currentLoginRemoteAddr; }

    @Override
    public Date getPreviousLoginDate() { return previousLoginDate; }

    @Override
    public String getPreviousLoginRemoteAddr() { return previousLoginRemoteAddr; }

    @Override
    public int getLoginCount() { return loginCount; }

    @Override
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String status) {
        this.userStatus = status;
    }

    @Override
    public Set<Privilege> getPrivileges() {
        return privileges;
    }

    @Override
    public String getCurrentWorkspaceId() {
        return currentWorkspaceId;
    }

    @Override
    public JSONObject getUiPreferences() { return preferences; }

    @Override
    public String toString() {
        return "SecureGraphUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "', privileges=" + getPrivileges() + "}";
    }
}
