package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.bigtable.model.user.accumulo.AccumuloUserContext;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import org.apache.accumulo.core.security.Authorizations;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public abstract class UserRepository {
    public static final String VISIBILITY_STRING = "user";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static final String LUMIFY_USER_CONCEPT_ID = "http://lumify.io/user";

    public abstract void init(Map config);

    public abstract User findByDisplayName(String username);

    public abstract Iterable<User> findAll();

    public abstract User findById(String userId);

    public abstract User addUser(String externalId, String displayName, String password, String[] userAuthorizations);

    public abstract void setPassword(User user, String password);

    public abstract boolean isPasswordValid(User user, String password);

    public abstract User setCurrentWorkspace(String userId, String workspaceId);

    public abstract User setStatus(String userId, UserStatus status);

    public abstract void addAuthorization(User userUser, String auth);

    public abstract void removeAuthorization(User userUser, String auth);

    public abstract com.altamiracorp.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations);

    public JSONObject toJsonWithAuths(User user) {
        JSONObject json = toJson(user);

        JSONArray authorizations = new JSONArray();
        for (String a : getAuthorizations(user).getAuthorizations()) {
            authorizations.put(a);
        }
        json.put("authorizations", authorizations);

        return json;
    }

    public JSONObject toJson(User user) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", user.getUserId());
            json.put("userName", user.getDisplayName());
            json.put("status", user.getUserStatus());
            json.put("userType", user.getUserType());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public ModelUserContext getModelUserContext(com.altamiracorp.securegraph.Authorizations authorizations, String... additionalAuthorizations) {
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

    public User getSystemUser ()
    {
        return new SystemUser(getModelUserContext(new String[0]));
    }
}
