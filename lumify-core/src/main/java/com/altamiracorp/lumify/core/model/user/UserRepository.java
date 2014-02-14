package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.VertexBuilder;
import com.altamiracorp.securegraph.Visibility;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.*;

@Singleton
public class UserRepository {
    public static final String VISIBILITY_STRING = "user";
    public static final Visibility VISIBILITY = new Visibility(VISIBILITY_STRING);
    public static final String LUMIFY_USER_CONCEPT_ID = "http://lumify.io/lumifyUser";
    private final Graph graph;
    private final String userConceptId;
    private final User authUser;

    @Inject
    public UserRepository(final Graph graph, final UserProvider userProvider, final OntologyRepository ontologyRepository) {
        this.graph = graph;
        this.authUser = userProvider.getUserManagerUser();

        Concept userConcept = ontologyRepository.getOrCreateConcept(null, LUMIFY_USER_CONCEPT_ID, "lumifyUser");
        userConceptId = userConcept.getId();
    }

    public Vertex findByUserName(String username) {
        return Iterables.getFirst(graph.query(authUser.getAuthorizations())
                .has(USERNAME.getKey(), username)
                .has(CONCEPT_TYPE.getKey(), userConceptId)
                .vertices(), null);
    }

    public Iterable<Vertex> findAll() {
        return graph.query(authUser.getAuthorizations())
                .has(CONCEPT_TYPE.getKey(), userConceptId)
                .vertices();
    }

    public Vertex findById(String userId) {
        return graph.getVertex(userId, authUser.getAuthorizations());
    }

    public Vertex addUser(String username, String password, String[] authorizations) {
        Vertex existingUser = findByUserName(username);
        if (existingUser != null) {
            throw new RuntimeException("");
        }

        String authorizationsString = StringUtils.join(authorizations, ",");

        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

        VertexBuilder userBuilder = graph.prepareVertex("USER_" + graph.getIdGenerator().nextId(), VISIBILITY, this.authUser.getAuthorizations());
        USERNAME.setProperty(userBuilder, username, VISIBILITY);
        CONCEPT_TYPE.setProperty(userBuilder, userConceptId, VISIBILITY);
        PASSWORD_SALT.setProperty(userBuilder, salt, VISIBILITY);
        PASSWORD_HASH.setProperty(userBuilder, passwordHash, VISIBILITY);
        STATUS.setProperty(userBuilder, UserStatus.OFFLINE.toString(), VISIBILITY);
        AUTHORIZATIONS.setProperty(userBuilder, authorizationsString, VISIBILITY);
        Vertex user = userBuilder.save();
        graph.flush();
        return user;
    }

    public void setPassword(Vertex user, String password) {
        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);
        PASSWORD_SALT.setProperty(user, salt, VISIBILITY);
        PASSWORD_HASH.setProperty(user, passwordHash, VISIBILITY);
        graph.flush();
    }

    public boolean isPasswordValid(Vertex user, String password) {
        try {
            return UserPasswordUtil.validatePassword(password, PASSWORD_SALT.getPropertyValue(user), PASSWORD_HASH.getPropertyValue(user));
        } catch (Exception ex) {
            throw new RuntimeException("error validating password", ex);
        }
    }

    public static JSONObject toJson(Vertex user) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", user.getId().toString());
            json.put("userName", USERNAME.getPropertyValue(user));
            json.put("status", STATUS.getPropertyValue(user).toLowerCase());
            json.put("userType", UserType.USER.toString());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Vertex setCurrentWorkspace(String userId, String workspaceRowKey) {
        Vertex user = findById(userId);
        if (user == null) {
            throw new RuntimeException("Could not find user: " + userId);
        }
        CURRENT_WORKSPACE.setProperty(user, workspaceRowKey, VISIBILITY);
        graph.flush();
        return user;
    }

    public Vertex setStatus(String userId, UserStatus status) {
        Vertex user = findById(userId);
        if (user == null) {
            throw new RuntimeException("Could not find user: " + userId);
        }
        STATUS.setProperty(user, status.toString(), VISIBILITY);
        graph.flush();
        return user;
    }
}
