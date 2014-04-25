package io.lumify.core.model.user;

import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.user.Roles;
import io.lumify.core.user.SystemUser;
import io.lumify.core.user.User;
import org.securegraph.Authorizations;
import org.securegraph.inmemory.InMemoryAuthorizations;

import java.util.*;

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
    public User addUser(String username, String displayName, String password, Collection<Roles> roles, String[] userAuthorizations) {
        InMemoryUser user = new InMemoryUser(username, displayName, password, roles, userAuthorizations);
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

    @Override
    public Set<Roles> getRoles(User user) {
        if (user instanceof SystemUser) {
            return Roles.ALL;
        }
        return ((InMemoryUser) user).getRoles();
    }
}
