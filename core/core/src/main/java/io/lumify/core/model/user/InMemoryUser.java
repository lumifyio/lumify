package io.lumify.core.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.user.User;
import io.lumify.web.clientapi.model.Privilege;
import io.lumify.web.clientapi.model.UserStatus;
import io.lumify.web.clientapi.model.UserType;
import org.json.JSONObject;

import java.util.*;

public class InMemoryUser implements User {
    private final String userId;
    private final String userName;
    private final String displayName;
    private final String emailAddress;
    private final Date createDate;
    private Set<Privilege> privileges;
    private final List<String> authorizations;
    private final String currentWorkspaceId;
    private JSONObject preferences;
    private Date currentLoginDate;
    private String currentLoginRemoteAddr;
    private Date previousLoginDate;
    private String previousLoginRemoteAddr;
    private int loginCount;
    private String passwordResetToken;
    private Date passwordResetTokenExpirationDate;

    public InMemoryUser(String userName, String displayName, String emailAddress, Set<Privilege> privileges, String[] authorizations, String currentWorkspaceId) {
        this.userId = UUID.randomUUID().toString();
        this.userName = userName;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.createDate = new Date();
        this.privileges = privileges;
        this.authorizations = new ArrayList<String>();
        Collections.addAll(this.authorizations, authorizations);
        this.currentWorkspaceId = currentWorkspaceId;
        this.preferences = new JSONObject();
    }

    @Override
    public ModelUserContext getModelUserContext() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public Date getCreateDate() {
        return createDate;
    }

    @Override
    public Date getCurrentLoginDate() {
        return currentLoginDate;
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        return currentLoginRemoteAddr;
    }

    @Override
    public Date getPreviousLoginDate() {
        return previousLoginDate;
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        return previousLoginRemoteAddr;
    }

    @Override
    public int getLoginCount() {
        return loginCount;
    }

    @Override
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public UserStatus getUserStatus() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String getCurrentWorkspaceId() {
        return this.currentWorkspaceId;
    }

    public Set<Privilege> getPrivileges() {
        return this.privileges;
    }

    public String[] getAuthorizations() {
        return authorizations.toArray(new String[this.authorizations.size()]);
    }

    public void setPrivileges(Set<Privilege> privileges) {
        this.privileges = privileges;
    }

    @Override
    public JSONObject getUiPreferences() {
        return preferences;
    }

    public void setPreferences(JSONObject preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        return passwordResetTokenExpirationDate;
    }
}
