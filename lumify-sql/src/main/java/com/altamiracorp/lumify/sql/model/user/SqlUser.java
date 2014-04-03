package com.altamiracorp.lumify.sql.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.lumify.core.user.User;

import javax.persistence.*;

@Entity
@Table(name = "user")
public class SqlUser implements User {
    private static final long serialVersionUID = 1L;
    private ModelUserContext modelUserContext;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private int id;
    @Column(name = "external_id")
    private String externalId;
    @Column(name = "display_name")
    private String displayName;
    @Column(name = "current_workspace")
    private String currentWorkspace;
    @Column(name = "password_salt")
    private byte[] passwordSalt;
    @Column(name = "password_hash")
    private byte[] passwordHash;
    @Column(name = "user_status")
    private String userStatus;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String userName) {
        this.displayName = userName;
    }

    public String getCurrentWorkspace() {
        return currentWorkspace;
    }

    public void setCurrentWorkspace(String currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
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

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    @Override
    @Transient
    public String getUserId() {
        return Integer.toString(id);
    }

    @Transient
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
