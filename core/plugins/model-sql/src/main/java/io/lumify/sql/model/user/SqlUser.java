package io.lumify.sql.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserType;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import io.lumify.sql.model.workspace.SqlWorkspace;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "user")
public class SqlUser implements User {
    private int id;
    private String username;
    private String passwordHash;
    private String passwordSalt;
    private String displayName;
    private String emailAddress;
    private Date createDate;
    private Date currentLoginDate;
    private String currentLoginRemoteAddr;
    private Date previousLoginDate;
    private String previousLoginRemoteAddr;
    private int loginCount;
    private String userStatus;
    private String privileges;
    private String uiPreferencesString;
    private SqlWorkspace currentWorkspace;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id", unique = true)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    @Transient
    public String getUserId() {
        return Integer.toString(id);
    }

    @Override
    @Column(name = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Column(name = "password_hash")
    public byte[] getPasswordHash() {
        return Base64.decodeBase64(passwordHash);
    }

    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = Base64.encodeBase64String(passwordHash);
    }

    @Column(name = "password_salt")
    public byte[] getPasswordSalt() {
        return Base64.decodeBase64(passwordSalt);
    }

    public void setPasswordSalt(byte[] passwordSalt) {
        this.passwordSalt = Base64.encodeBase64String(passwordSalt);
    }

    @Override
    @Column(name = "display_name")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    @Column(name = "email_address")
    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    @Column(name = "create_date")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Override
    @Column(name = "current_login_date")
    public Date getCurrentLoginDate() {
        return currentLoginDate;
    }

    public void setCurrentLoginDate(Date currentLoginDate) {
        this.currentLoginDate = currentLoginDate;
    }

    @Override
    @Column(name = "current_login_remote_addr")
    public String getCurrentLoginRemoteAddr() {
        return currentLoginRemoteAddr;
    }

    public void setCurrentLoginRemoteAddr(String currentLoginRemoteAddr) {
        this.currentLoginRemoteAddr = currentLoginRemoteAddr;
    }

    @Override
    @Column(name = "previous_login_date")
    public Date getPreviousLoginDate() {
        return previousLoginDate;
    }

    public void setPreviousLoginDate(Date previousLoginDate) {
        this.previousLoginDate = previousLoginDate;
    }

    @Override
    @Column(name = "previous_login_remote_addr")
    public String getPreviousLoginRemoteAddr() {
        return previousLoginRemoteAddr;
    }

    public void setPreviousLoginRemoteAddr(String previousLoginRemoteAddr) {
        this.previousLoginRemoteAddr = previousLoginRemoteAddr;
    }

    @Override
    @Column(name = "login_count")
    public int getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

    @Override
    @Transient
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    @Column(name = "user_status")
    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    @Override
    @Transient
    public String getCurrentWorkspaceId() {
        return currentWorkspace != null ? currentWorkspace.getId() : null;
    }

    @OneToOne
    @JoinColumn(referencedColumnName = "workspace_id", name="current_workspace_id")
    public SqlWorkspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    public void setCurrentWorkspace(SqlWorkspace currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
    }

    @Override
    @Transient
    public JSONObject getUiPreferences() {
        return uiPreferencesString != null ? new JSONObject(uiPreferencesString) : null;
    }

    public void setUiPreferences(JSONObject uiPreferences) {
        uiPreferencesString = uiPreferences.toString();
    }

    @Column(name = "ui_preferences")
    public String getUiPreferencesString() {
        return uiPreferencesString;
    }

    public void setUiPreferencesString(String uiPreferencesString) {
        this.uiPreferencesString = uiPreferencesString;
    }

    @Override
    @Transient
    public Set<Privilege> getPrivileges() {
        return Privilege.stringToPrivileges(privileges);
    }

    @Column(name = "privileges")
    public String getPrivilegesString() {
        return privileges;
    }

    public void setPrivilegesString(String privileges) {
        this.privileges = privileges;
    }

    @Override
    public String toString() {
        return "SqlUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "', privileges=" + getPrivilegesString() + "}";
    }

    @Override
    @Transient
    public ModelUserContext getModelUserContext() {
        return null;
    }
}
