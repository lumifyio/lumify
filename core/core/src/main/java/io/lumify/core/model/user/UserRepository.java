package io.lumify.core.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.bigtable.model.user.accumulo.AccumuloUserContext;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.longRunningProcess.LongRunningProcessRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.SystemUser;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.core.util.JSONUtil;
import io.lumify.web.clientapi.model.ClientApiUser;
import io.lumify.web.clientapi.model.Privilege;
import io.lumify.web.clientapi.model.UserStatus;
import org.apache.accumulo.core.security.Authorizations;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.securegraph.util.IterableUtils.toList;

public abstract class UserRepository {
    public static final String VISIBILITY_STRING = "user";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static final String USER_CONCEPT_IRI = "http://lumify.io/user";
    private final Set<Privilege> defaultPrivileges;
    private LongRunningProcessRepository longRunningProcessRepository; // can't inject this because of circular dependencies

    @Inject
    protected UserRepository(Configuration configuration) {
        this.defaultPrivileges = Privilege.stringToPrivileges(configuration.get(Configuration.DEFAULT_PRIVILEGES, ""));
    }

    public abstract User findByUsername(String username);

    public abstract Iterable<User> find(int skip, int limit);

    /*
    simple and likely slow implementation expected to be overridden in production implementations
     */
    public Iterable<User> findByStatus(int skip, int limit, UserStatus status) {
        List<User> allUsers = toList(find(skip, limit));
        List<User> matchingUsers = new ArrayList<User>();
        for (User user : allUsers) {
            if (user.getUserStatus() == status) {
                matchingUsers.add(user);
            }
        }
        return matchingUsers;
    }

    public abstract User findById(String userId);

    public abstract User addUser(String username, String displayName, String emailAddress, String password, String[] userAuthorizations);

    public abstract void setPassword(User user, String password);

    public abstract boolean isPasswordValid(User user, String password);

    public abstract void recordLogin(User user, String remoteAddr);

    public abstract User setCurrentWorkspace(String userId, String workspaceId);

    public abstract String getCurrentWorkspaceId(String userId);

    public abstract User setStatus(String userId, UserStatus status);

    public abstract void addAuthorization(User userUser, String auth);

    public abstract void removeAuthorization(User userUser, String auth);

    public abstract org.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations);

    public abstract void setDisplayName(User user, String displayName);

    public abstract void setEmailAddress(User user, String emailAddress);

    public abstract Set<Privilege> getPrivileges(User user);

    public abstract void setUiPreferences(User user, JSONObject preferences);

    public JSONObject toJsonWithAuths(User user) {
        JSONObject json = toJson(user);

        JSONArray authorizations = new JSONArray();
        for (String a : getAuthorizations(user).getAuthorizations()) {
            authorizations.put(a);
        }
        json.put("authorizations", authorizations);

        json.put("uiPreferences", user.getUiPreferences());

        Set<Privilege> privileges = getPrivileges(user);
        json.put("privileges", Privilege.toJson(privileges));

        return json;
    }

    /**
     * This is different from the non-private method in that it returns authorizations,
     * long running processes, etc for that user.
     */
    public ClientApiUser toClientApiPrivate(User user) {
        ClientApiUser u = toClientApi(user);

        for (String a : getAuthorizations(user).getAuthorizations()) {
            u.addAuthorization(a);
        }

        for (JSONObject json : getLongRunningProcesses(user)) {
            u.getLongRunningProcesses().add(ClientApiConverter.toClientApiValue(json));
        }

        u.setUiPreferences(JSONUtil.toJsonNode(user.getUiPreferences()));

        Set<Privilege> privileges = getPrivileges(user);
        u.getPrivileges().addAll(privileges);

        return u;
    }

    private List<JSONObject> getLongRunningProcesses(User user) {
        return getLongRunningProcessRepository().getLongRunningProcesses(user);
    }

    private LongRunningProcessRepository getLongRunningProcessRepository() {
        if (this.longRunningProcessRepository == null) {
            this.longRunningProcessRepository = InjectHelper.getInstance(LongRunningProcessRepository.class);
        }
        return this.longRunningProcessRepository;
    }

    private ClientApiUser toClientApi(User user) {
        return toClientApi(user, null);
    }

    private ClientApiUser toClientApi(User user, Map<String, String> workspaceNames) {
        ClientApiUser u = new ClientApiUser();
        u.setId(user.getUserId());
        u.setUserName(user.getUsername());
        u.setDisplayName(user.getDisplayName());
        u.setStatus(user.getUserStatus());
        u.setUserType(user.getUserType());
        u.setEmail(user.getEmailAddress());
        u.setCurrentWorkspaceId(user.getCurrentWorkspaceId());
        if (workspaceNames != null) {
            String workspaceName = workspaceNames.get(user.getCurrentWorkspaceId());
            u.setCurrentWorkspaceName(workspaceName);
        }
        return u;
    }

    protected String formatUsername(String username) {
        return username.trim().toLowerCase();
    }

    public static JSONArray toJson(Iterable<User> users) throws JSONException {
        return toJson(users, null);
    }

    public static JSONArray toJson(Iterable<User> users, Map<String, String> workspaceNames) {
        JSONArray usersJson = new JSONArray();
        for (User user : users) {
            usersJson.put(UserRepository.toJson(user, workspaceNames));
        }
        return usersJson;
    }

    public static JSONObject toJson(User user) {
        return toJson(user, null);
    }

    public static JSONObject toJson(User user, Map<String, String> workspaceNames) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", user.getUserId());
            json.put("userName", user.getUsername());
            json.put("displayName", user.getDisplayName());
            json.put("status", user.getUserStatus());
            json.put("userType", user.getUserType());
            json.put("email", user.getEmailAddress());
            json.put("currentWorkspaceId", user.getCurrentWorkspaceId());
            if (workspaceNames != null) {
                String workspaceName = workspaceNames.get(user.getCurrentWorkspaceId());
                json.put("currentWorkspaceName", workspaceName);
            }
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

    public User findOrAddUser(String username, String displayName, String emailAddress, String password, String[] authorizations) {
        User user = findByUsername(username);
        if (user == null) {
            user = addUser(username, displayName, emailAddress, password, authorizations);
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

        int skip = 0;
        int limit = 100;
        List<User> foundUsers = new ArrayList<User>();
        while (true) {
            List<User> users = toList(find(skip, limit));
            if (users.size() == 0) {
                break;
            }
            for (User user : users) {
                if (lowerCaseQuery == null || user.getDisplayName().toLowerCase().contains(lowerCaseQuery)) {
                    foundUsers.add(user);
                }
            }
            skip += limit;
        }
        return foundUsers;
    }

    public static String createRandomPassword() {
        return new BigInteger(120, new SecureRandom()).toString(32);
    }
}
