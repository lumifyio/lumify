package io.lumify.sql.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserType;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import io.lumify.sql.model.workspace.SqlWorkspace;
import io.lumify.sql.model.workspace.SqlWorkspaceUser;
import org.apache.commons.codec.binary.Base64;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user")
public class SqlUser implements User {
    @Transient
    private ModelUserContext modelUserContext;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id", unique = true)
    private int id;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "password_salt")
    private String passwordSalt;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "user_status")
    private String userStatus;

    @Column(name = "privileges")
    private int privileges;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn(name = "workspace_id")
    private SqlWorkspace currentWorkspace;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "sqlWorkspaceUserId.user", cascade = CascadeType.ALL)
    private Set<SqlWorkspaceUser> sqlWorkspaceUsers = new HashSet<SqlWorkspaceUser>(0);

    public SqlUser() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String userName) {
        this.displayName = userName;
    }

    public SqlWorkspace getCurrentWorkspace() {
        return currentWorkspace;
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

    public String getUserStatus() {
        return userStatus;
    }

    public Set<Privilege> getPrivileges() {
        return Privilege.toSet(this.privileges);
    }

    public void setPrivileges(int privileges) {
        this.privileges = privileges;
    }

    public void setCurrentWorkspace(Workspace currentWorkspace) {
        this.currentWorkspace = (SqlWorkspace) currentWorkspace;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public Set<SqlWorkspaceUser> getSqlWorkspaceUsers() {
        return sqlWorkspaceUsers;
    }

    public void setSqlWorkspaceUsers(Set<SqlWorkspaceUser> sqlWorkspaceUsers) {
        this.sqlWorkspaceUsers = sqlWorkspaceUsers;
    }

    @Transient
    public String getUserId() {
        return Integer.toString(id);
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public void setModelUserContext(ModelUserContext modelUserContext) {
        this.modelUserContext = modelUserContext;
    }

    @Transient
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public String toString() {
        return "SqlUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "'}";
    }
}
