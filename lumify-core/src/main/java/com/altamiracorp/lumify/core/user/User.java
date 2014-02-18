package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.securegraph.Authorizations;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private Set<String> authorizationsSet;
    private final AuthorizationBuilder authorizationBuilder;
    private String username;
    private String userId;
    private String currentWorkspace;
    private ModelUserContext modelUserContext;
    private UserType userType;

    public User(String userId, String username, String currentWorkspace, ModelUserContext modelUserContext, UserType userType, AuthorizationBuilder authorizationBuilder, String[] authorizationsArray) {
        this(userId, username, currentWorkspace, modelUserContext, userType, authorizationBuilder, toSet(authorizationsArray));
    }

    private static Set<String> toSet(String[] authorizationsArray) {
        Set<String> r = new HashSet<String>();
        Collections.addAll(r, authorizationsArray);
        return r;
    }

    public User(String userId, String username, String currentWorkspace, ModelUserContext modelUserContext, UserType userType, AuthorizationBuilder authorizationBuilder, Set<String> authorizationsSet) {
        this.userId = userId;
        this.username = username;
        this.currentWorkspace = currentWorkspace;
        this.modelUserContext = modelUserContext;
        this.userType = userType;
        this.authorizationBuilder = authorizationBuilder;
        this.authorizationsSet = authorizationsSet;
    }

    public String getUserId() {
        return userId;
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public String getUsername() {
        return username;
    }

    public String getCurrentWorkspace() {
        return currentWorkspace;
    }

    public void setCurrentWorkspace(String currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setAuthorizationsSet(Set<String> authorizationsSet) {
        this.authorizationsSet = authorizationsSet;
    }

    public Authorizations getAuthorizations() {
        return authorizationBuilder.create(this.authorizationsSet);
    }

    public Authorizations getAuthorizations(String... additionalAuthorizations) {
        Set<String> authorizationsArrayWithAdditional = new HashSet<String>(this.authorizationsSet);
        Collections.addAll(authorizationsArrayWithAdditional, additionalAuthorizations);
        return authorizationBuilder.create(authorizationsArrayWithAdditional);
    }
}
