package io.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserRepository;
import io.lumify.web.clientapi.model.Privilege;
import io.lumify.web.clientapi.model.UserStatus;
import io.lumify.web.clientapi.model.UserType;
import org.json.JSONObject;

import java.util.Date;
import java.util.Set;

/**
 * This class is used to store the userId only in a web session. If we were to store the entire
 * user object in the session, any changes to the user would not be reflected unless the user object
 * was refreshed.
 */
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
        return userId;
    }

    public User getProxiedUser() {
        ensureUser();
        return proxiedUser;
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
    public String getEmailAddress() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getEmailAddress();
    }

    @Override
    public Date getCreateDate() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getCreateDate();
    }

    @Override
    public Date getCurrentLoginDate() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getCurrentLoginDate();
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getCurrentLoginRemoteAddr();
    }

    @Override
    public Date getPreviousLoginDate() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getPreviousLoginDate();
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getPreviousLoginRemoteAddr();
    }

    @Override
    public int getLoginCount() {
        ensureUser();
        if (proxiedUser == null) {
            return 0;
        }
        return proxiedUser.getLoginCount();
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
    public UserStatus getUserStatus() {
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

    @Override
    public JSONObject getUiPreferences() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getUiPreferences();
    }

    @Override
    public Set<Privilege> getPrivileges() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getPrivileges();
    }

    @Override
    public String getPasswordResetToken() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getPasswordResetToken();
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getPasswordResetTokenExpirationDate();
    }

    private void ensureUser() {
        if (proxiedUser == null) {
            proxiedUser = userRepository.findById(userId);
        }
    }
}
