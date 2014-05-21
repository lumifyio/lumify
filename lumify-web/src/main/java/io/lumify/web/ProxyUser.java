package io.lumify.web;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.user.UserType;
import io.lumify.core.user.User;

public class ProxyUser implements User {
    private final String userId;
    private final UserRepository userRepository;
    private User proxiedUser;

    public ProxyUser(String userId, UserRepository userRepository) {
        this.userId = userId;
        this.userRepository = userRepository;
    }

    @Override
    public String getUserId() {
        return this.userId;
    }

    @Override
    public ModelUserContext getModelUserContext() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getModelUserContext();
    }

    @Override
    public String getUsername() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getUsername();
    }

    @Override
    public String getDisplayName() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getDisplayName();
    }

    @Override
    public UserType getUserType() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getUserType();
    }

    @Override
    public String getUserStatus() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getUserStatus();
    }

    @Override
    public String getCurrentWorkspaceId() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getCurrentWorkspaceId();
    }

    private void ensureUser() {
        if (proxiedUser == null) {
            proxiedUser = userRepository.findById(userId);
        }
    }
}
