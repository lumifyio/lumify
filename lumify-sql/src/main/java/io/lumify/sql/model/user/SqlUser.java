package io.lumify.sql.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserType;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import io.lumify.sql.model.workspace.SqlWorkspace;
import io.lumify.sql.model.workspace.SqlWorkspaceUser;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user")
public class SqlUser implements User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id", unique = true)
    private int id;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "password_salt")
    private String passwordSalt;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "create_date")
    private Date createDate;

    @Column(name = "current_login_date")
    private Date currentLoginDate;

    @Column(name = "current_login_remote_addr")
    public String currentLoginRemoteAddr;

    @Column(name = "previous_login_date")
    public Date previousLoginDate;

    @Column(name = "previous_login_remote_addr")
    public String previousLoginRemoteAddr;

    @Column(name = "login_count")
    public int loginCount;

    @Column(name = "user_status")
    private String userStatus;

    @Column(name = "privileges")
    private String privileges;

    @Column(name = "ui_preferences")
    private String uiPreferences;

    @OneToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn(name = "workspace_id")
    private SqlWorkspace currentWorkspace;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "sqlWorkspaceUserId.user", cascade = CascadeType.ALL)
    private Set<SqlWorkspaceUser> sqlWorkspaceUsers = new HashSet<SqlWorkspaceUser>(0);

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ModelUserContext getModelUserContext() {
        return null;
    }

    @Transient
    @Override
    public String getUserId() {
        return Integer.toString(id);
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getPasswordHash() {
        return Base64.decodeBase64(passwordHash);
    }

    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = Base64.encodeBase64String(passwordHash);
    }

    public byte[] getPasswordSalt() {
        return Base64.decodeBase64(passwordSalt);
    }

    public void setPasswordSalt(byte[] passwordSalt) {
        this.passwordSalt = Base64.encodeBase64String(passwordSalt);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String userName) {
        this.displayName = userName;
    }

    @Override
    public String getEmailAddress() { return emailAddress; }

    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    @Override
    public Date getCreateDate() { return createDate; }

    public void setCreateDate(Date createDate) { this.createDate = createDate; }

    @Override
    public Date getCurrentLoginDate() { return currentLoginDate; }

    public void setCurrentLoginDate(Date currentLoginDate) { this.currentLoginDate = currentLoginDate; }

    @Override
    public String getCurrentLoginRemoteAddr() { return currentLoginRemoteAddr; }

    public void setCurrentLoginRemoteAddr(String currentLoginRemoteAddr) { this.currentLoginRemoteAddr = currentLoginRemoteAddr; }

    @Override
    public Date getPreviousLoginDate() { return previousLoginDate; }

    public void setPreviousLoginDate(Date previousLoginDate) { this.previousLoginDate = previousLoginDate; }

    @Override
    public String getPreviousLoginRemoteAddr() { return previousLoginRemoteAddr; }

    public void setPreviousLoginRemoteAddr(String previousLoginRemoteAddr) { this.previousLoginRemoteAddr = previousLoginRemoteAddr; }

    @Override
    public int getLoginCount() { return loginCount; }

    public void setLoginCount(int loginCount) { this.loginCount = loginCount; }

    @Transient
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public Set<Privilege> getPrivileges() {
        return Privilege.stringToPrivileges(privileges);
    }

    public void setPrivileges(String privileges) {
        this.privileges = privileges;
    }

    public SqlWorkspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    @Override
    public String getCurrentWorkspaceId() {
        SqlWorkspace workspace = getCurrentWorkspace();
        if (workspace == null) {
            return null;
        }
        return workspace.getId();
    }

    public void setCurrentWorkspace(Workspace currentWorkspace) {
        this.currentWorkspace = (SqlWorkspace) currentWorkspace;
    }

    @Override
    public JSONObject getUiPreferences() { return uiPreferences != null ? new JSONObject(uiPreferences) : null; }

    public void setUiPreferences(JSONObject uiPreferences) { this.uiPreferences = uiPreferences.toString(); }

    public Set<SqlWorkspaceUser> getSqlWorkspaceUsers() {
        return sqlWorkspaceUsers;
    }

    @Override
    public String toString() {
        return "SqlUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "'}";
    }
}
