package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.VertexBuilder;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.*;

@Singleton
public class UserRepository {
    public static final String VISIBILITY_STRING = "user";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static final String LUMIFY_USER_CONCEPT_ID = "http://lumify.io/user";
    private final Graph graph;
    private final com.altamiracorp.securegraph.Authorizations authorizations;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public UserRepository(
            final Graph graph,
            final OntologyRepository ontologyRepository,
            final AuthorizationRepository authorizationRepository) {
        this.graph = graph;
        this.authorizationRepository = authorizationRepository;

        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        authorizationRepository.addAuthorizationToGraph(LumifyVisibility.VISIBILITY_STRING);

        Concept userConcept = ontologyRepository.getOrCreateConcept(null, LUMIFY_USER_CONCEPT_ID, "lumifyUser");

        Set<String> authorizationsSet = new HashSet<String>();
        authorizationsSet.add(VISIBILITY_STRING);
        authorizationsSet.add(LumifyVisibility.VISIBILITY_STRING);
        this.authorizations = authorizationRepository.createAuthorizations(authorizationsSet);
    }

    public Vertex findByUserName(String username) {
        return Iterables.getFirst(graph.query(authorizations)
                .has(USERNAME.getKey(), username)
                .has(CONCEPT_TYPE.getKey(), LUMIFY_USER_CONCEPT_ID)
                .vertices(), null);
    }

    public Iterable<Vertex> findAll() {
        return graph.query(authorizations)
                .has(CONCEPT_TYPE.getKey(), LUMIFY_USER_CONCEPT_ID)
                .vertices();
    }

    public Vertex findById(String userId) {
        return graph.getVertex(userId, authorizations);
    }

    public Vertex addUser(String username, String password, String[] userAuthorizations) {
        Vertex existingUser = findByUserName(username);
        if (existingUser != null) {
            throw new RuntimeException("");
        }

        String authorizationsString = StringUtils.join(userAuthorizations, ",");

        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

        String userId = "USER_" + graph.getIdGenerator().nextId();
        VertexBuilder userBuilder = graph.prepareVertex(userId, VISIBILITY.getVisibility(), this.authorizations);
        USERNAME.setProperty(userBuilder, username, VISIBILITY.getVisibility());
        CONCEPT_TYPE.setProperty(userBuilder, LUMIFY_USER_CONCEPT_ID, VISIBILITY.getVisibility());
        PASSWORD_SALT.setProperty(userBuilder, salt, VISIBILITY.getVisibility());
        PASSWORD_HASH.setProperty(userBuilder, passwordHash, VISIBILITY.getVisibility());
        STATUS.setProperty(userBuilder, UserStatus.OFFLINE.toString(), VISIBILITY.getVisibility());
        AUTHORIZATIONS.setProperty(userBuilder, authorizationsString, VISIBILITY.getVisibility());
        Vertex user = userBuilder.save();
        graph.flush();
        return user;
    }

    public void setPassword(Vertex user, String password) {
        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);
        PASSWORD_SALT.setProperty(user, salt, VISIBILITY.getVisibility());
        PASSWORD_HASH.setProperty(user, passwordHash, VISIBILITY.getVisibility());
        graph.flush();
    }

    public boolean isPasswordValid(Vertex user, String password) {
        try {
            return UserPasswordUtil.validatePassword(password, PASSWORD_SALT.getPropertyValue(user), PASSWORD_HASH.getPropertyValue(user));
        } catch (Exception ex) {
            throw new RuntimeException("error validating password", ex);
        }
    }

    public JSONObject toJson(Vertex userVertex, User user) {
        JSONObject json = toJson(userVertex);

        JSONArray authorizations = new JSONArray();
        for (String a : getAuthorizations(user).getAuthorizations()) {
            authorizations.put(a);
        }
        json.put("authorizations", authorizations);

        return json;
    }

    public static JSONObject toJson(Vertex userVertex) {
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

    public Vertex setCurrentWorkspace(String userId, String workspaceId) {
        Vertex user = findById(userId);
        if (user == null) {
            throw new RuntimeException("Could not find user: " + userId);
        }
        CURRENT_WORKSPACE.setProperty(user, workspaceId, VISIBILITY.getVisibility());
        graph.flush();
        return user;
    }

    public Vertex setStatus(String userId, UserStatus status) {
        Vertex user = findById(userId);
        if (user == null) {
            throw new RuntimeException("Could not find user: " + userId);
        }
        STATUS.setProperty(user, status.toString(), VISIBILITY.getVisibility());
        graph.flush();
        return user;
    }

    public void addAuthorization(Vertex userVertex, String auth) {
        Set<String> authorizations = UserLumifyProperties.getAuthorizations(userVertex);
        if (authorizations.contains(auth)) {
            return;
        }
        authorizations.add(auth);

        this.authorizationRepository.addAuthorizationToGraph(auth);

        String authorizationsString = StringUtils.join(authorizations, ",");
        AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY.getVisibility());
        graph.flush();
    }

    public void removeAuthorization(Vertex userVertex, String auth) {
        Set<String> authorizations = UserLumifyProperties.getAuthorizations(userVertex);
        if (!authorizations.contains(auth)) {
            return;
        }
        authorizations.remove(auth);
        String authorizationsString = StringUtils.join(authorizations, ",");
        AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY.getVisibility());
        graph.flush();
    }

    public com.altamiracorp.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations) {
        Vertex userVertex = findById(user.getUserId());
        Set<String> authorizationsSet = UserLumifyProperties.getAuthorizations(userVertex);
        Collections.addAll(authorizationsSet, additionalAuthorizations);
        return authorizationRepository.createAuthorizations(authorizationsSet);
    }
}
