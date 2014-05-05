package io.lumify.core.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.bigtable.model.user.accumulo.AccumuloUserContext;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.SystemUser;
import io.lumify.core.user.User;
import org.apache.accumulo.core.security.Authorizations;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.util.FilterIterable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Set;

public abstract class UserRepository {
    public static final String VISIBILITY_STRING = "user";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static final String LUMIFY_USER_CONCEPT_ID = "http://lumify.io/user";
    private final Set<Privilege> defaultPrivileges;

    @Inject
    protected UserRepository(Configuration configuration) {
        this.defaultPrivileges = Privilege.stringToPrivileges(configuration.get(Configuration.DEFAULT_PRIVILEGES, ""));
    }

    public abstract User findByUsername(String username);

    public abstract Iterable<User> findAll();

    public abstract User findById(String userId);

    public abstract User addUser(String username, String displayName, String password, String[] userAuthorizations);

    public abstract void setPassword(User user, String password);

    public abstract boolean isPasswordValid(User user, String password);

    public abstract User setCurrentWorkspace(String userId, String workspaceId);

    public abstract String getCurrentWorkspaceId(String userId);

    public abstract User setStatus(String userId, UserStatus status);

    public abstract void addAuthorization(User userUser, String auth);

    public abstract void removeAuthorization(User userUser, String auth);

    public abstract org.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations);

    public abstract Set<Privilege> getPrivileges(User user);

    public JSONObject toJsonWithAuths(User user) {
        JSONObject json = toJson(user);

        JSONArray authorizations = new JSONArray();
        for (String a : getAuthorizations(user).getAuthorizations()) {
            authorizations.put(a);
        }
        json.put("authorizations", authorizations);

        Set<Privilege> privileges = getPrivileges(user);
        json.put("privileges", Privilege.toJson(privileges));

        return json;
    }

    public static JSONObject toJson(User user) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", user.getUserId());
            json.put("userName", user.getDisplayName());
            json.put("status", user.getUserStatus());
            json.put("userType", user.getUserType());
            json.put("currentWorkspaceId", user.getCurrentWorkspaceId());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public ModelUserContext getModelUserContext(org.securegraph.Authorizations authorizations, String... additionalAuthorizations) {
        ArrayList<String> auths = new ArrayList<String>();

        if (authorizations.getAuthorizations() != null) {
            for (String a : authorizations.getAuthorizations()) {
                if (a != null && a.length() > 0) {
                    auths.add(a);
                }
            }
        }

        if (additionalAuthorizations != null) {
            for (String a : additionalAuthorizations) {
                if (a != null && a.length() > 0) {
                    auths.add(a);
                }
            }
        }

        return getModelUserContext(auths.toArray(new String[auths.size()]));
    }

    public ModelUserContext getModelUserContext(String... authorizations) {
        // TODO: figure out a better way to create this without requiring accumulo
        return new AccumuloUserContext(new Authorizations(authorizations));
    }

    public User getSystemUser() {
        return new SystemUser(getModelUserContext(LumifyVisibility.SUPER_USER_VISIBILITY_STRING));
    }

    public User findOrAddUser(String username, String displayName, String password, String[] authorizations) {
        User user = findByUsername(username);
        if (user == null) {
            user = addUser(username, displayName, password, authorizations);
        }
        return user;
    }

    public Set<Privilege> getDefaultPrivileges() {
        return defaultPrivileges;
    }

    public abstract void delete(User user);

    public abstract void setPrivileges(User user, Set<Privilege> privileges);

    public Iterable<User> find(String query) {
        final String lowerCaseQuery = query == null ? null : query.toLowerCase();
        return new FilterIterable<User>(findAll()) {
            @Override
            protected boolean isIncluded(User user) {
                if (lowerCaseQuery == null) {
                    return true;
                }
                return user.getDisplayName().toLowerCase().contains(lowerCaseQuery);
            }
        };
    }
}
