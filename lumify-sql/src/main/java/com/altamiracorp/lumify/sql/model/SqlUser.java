package com.altamiracorp.lumify.sql.model;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.lumify.core.user.User;

import javax.persistence.*;

@Entity
@Table(name = "user")
@SequenceGenerator(name = "id_gen", allocationSize = 1)
public class SqlUser implements User {
    private static final long serialVersionUID = 1L;
    private ModelUserContext modelUserContext;
    private String currentWorkspace;
    private int id;
    private String externalId;
    private String displayName;
    private byte[] passwordSalt;
    private byte[] passwordHash;
    private String userStatus;

    public SqlUser() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_gen")
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "external_id")
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Column(name = "display_name")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String userName) {
        this.displayName = userName;
    }

    @Column(name = "current_workspace")
    public String getCurrentWorkspace() {
        return currentWorkspace;
    }

    public void setCurrentWorkspace(String currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
    }

    @Column(name = "password_hash")
    public byte[] getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Column(name = "password_salt")
    public byte[] getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(byte[] passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    @Column(name = "user_status")
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
