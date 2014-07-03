package io.lumify.core.model.user;

import io.lumify.core.config.Configuration;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.SystemUser;
import io.lumify.core.user.User;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.inmemory.InMemoryAuthorizations;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InMemoryUserRepository extends UserRepository {
    private UserListenerUtil userListenerUtil;

    @Inject
    public InMemoryUserRepository(Configuration configuration, UserListenerUtil userListenerUtil) {
        super(configuration);
        this.userListenerUtil = userListenerUtil;
    }

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
    public User addUser(String username, String displayName, String emailAddress, String password, String[] userAuthorizations) {
        InMemoryUser user = new InMemoryUser(username, displayName, emailAddress, getDefaultPrivileges(), userAuthorizations, null);
        userListenerUtil.fireNewUserAddedEvent(user);
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
    public void recordLogin(User user, String remoteAddr) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User setCurrentWorkspace(String userId, String workspaceId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getCurrentWorkspaceId(String userId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setUiPreferences(User user, JSONObject preferences) {
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
    public Set<Privilege> getPrivileges(User user) {
        if (user instanceof SystemUser) {
            return Privilege.ALL;
        }
        return ((InMemoryUser) user).getPrivileges();
    }

    @Override
    public void delete(User user) {
    }

    @Override
    public void setPrivileges(User user, Set<Privilege> privileges) {
        ((InMemoryUser) user).setPrivileges(privileges);
    }
}
