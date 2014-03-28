package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.STATUS;
import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.USERNAME;

public abstract class UserRepository {
    public static final String VISIBILITY_STRING = "user";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static final String LUMIFY_USER_CONCEPT_ID = "http://lumify.io/user";

    public abstract void init(Map map);

    public abstract Vertex findByUserName(String username);

    public abstract Iterable<Vertex> findAll();

    public abstract Vertex findById(String userId);

    public abstract Vertex addUser(String username, String password, String[] userAuthorizations);

    public abstract void setPassword(Vertex user, String password);

    public abstract boolean isPasswordValid(Vertex user, String password);

    public JSONObject toJson(Vertex userVertex, User user){
        JSONObject json = toJson(userVertex);

        JSONArray authorizations = new JSONArray();
        for (String a : getAuthorizations(user).getAuthorizations()) {
            authorizations.put(a);
        }
        json.put("authorizations", authorizations);

        return json;
    }

    public JSONObject toJson(Vertex userVertex) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", userVertex.getId().toString());
            json.put("userName", USERNAME.getPropertyValue(userVertex));
            json.put("status", STATUS.getPropertyValue(userVertex).toLowerCase());
            json.put("userType", UserType.USER.toString());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract Vertex setCurrentWorkspace(String userId, String workspaceId);

    public abstract Vertex setStatus(String userId, UserStatus status);

    public abstract void addAuthorization(Vertex userVertex, String auth);

    public abstract void removeAuthorization(Vertex userVertex, String auth);

    public abstract com.altamiracorp.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations);
}
