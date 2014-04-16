package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.inmemory.InMemoryAuthorizations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryUserRepository extends UserRepository {
    private final List<InMemoryUser> users = new ArrayList<InMemoryUser>();

    @Override
    public User findByUsername(String username) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Iterable<User> findAll() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User findById(String userId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User addUser(String username, String displayName, String password, String[] userAuthorizations) {
        InMemoryUser user = new InMemoryUser(username, displayName, password, userAuthorizations);
        users.add(user);
        return user;
    }

    @Override
    public void setPassword(User user, String password) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isPasswordValid(User user, String password) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User setCurrentWorkspace(String userId, Workspace workspace) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getCurrentWorkspaceId(String userId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User setStatus(String userId, UserStatus status) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void addAuthorization(User userUser, String auth) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeAuthorization(User userUser, String auth) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Authorizations getAuthorizations(User user, String... additionalAuthorizations) {
        List<String> auths = new ArrayList<String>();
        Collections.addAll(auths, ((InMemoryUser) user).getAuthorizations());
        Collections.addAll(auths, additionalAuthorizations);
        return new InMemoryAuthorizations(auths.toArray(new String[auths.size()]));
    }
}
