package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.user.User;

import javax.persistence.*;

@Entity
@Table(name="SQL_USER")
public class SqlUser implements User {
    private ModelUserContext modelUserContext;
    private String currentWorkspace;
    private int id;
    private String externalId;
    private String userName;

    public SqlUser () {};

    @Id
    @GeneratedValue
    @Column(name="USER_ID")
    public int getId () {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name="EXTERNAL_ID")
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Column(name="NAME")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column(name="CURRENT_WORKSPACE")
    public String getCurrentWorkspace() {
        return currentWorkspace;
    }

    public void setCurrentWorkspace(String currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
    }

    public String getUserId() {
        return Integer.toString(id);
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.OFFLINE;
    }
}
