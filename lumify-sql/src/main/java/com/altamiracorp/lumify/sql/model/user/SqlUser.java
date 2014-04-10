package com.altamiracorp.lumify.sql.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.sql.model.workspace.SqlWorkspace;
import com.altamiracorp.lumify.sql.model.workspace.SqlWorkspaceUser;

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
    @Column(name = "external_id", unique = true)
    private String externalId;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "password_salt")
    private byte[] passwordSalt;
    @Column(name = "password_hash")
    private byte[] passwordHash;
    @Column(name = "user_status")
    private String userStatus;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn (name="workspace_id")
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

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getUserName() {
        return userName;
    }

    public void setDisplayName(String userName) {
        this.userName = userName;
    }

    public SqlWorkspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    public byte[] getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = passwordHash;
    }

    public byte[] getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(byte[] passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getUserStatus() {
        return userStatus;
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
}
