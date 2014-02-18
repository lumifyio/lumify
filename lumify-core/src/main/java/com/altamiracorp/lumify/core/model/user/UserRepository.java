package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.AuthorizationBuilder;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.VertexBuilder;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.accumulo.AccumuloGraph;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.*;

@Singleton
public class UserRepository {
    public static final String VISIBILITY_STRING = "user";
    public static final Visibility VISIBILITY = new Visibility(VISIBILITY_STRING);
    public static final String LUMIFY_USER_CONCEPT_ID = "http://lumify.io/user";
    private final Graph graph;
    private final String userConceptId;
    private final com.altamiracorp.securegraph.Authorizations authorizations;
    private final AuthorizationBuilder authorizationBuilder;

    @Inject
    public UserRepository(final Graph graph, final OntologyRepository ontologyRepository, final AuthorizationBuilder authorizationBuilder) {
        this.graph = graph;
        this.authorizationBuilder = authorizationBuilder;

        addAuthorizationToGraph(VISIBILITY_STRING);
        addAuthorizationToGraph(OntologyRepository.VISIBILITY_STRING); // This can't be moved to the OntologyRepository because it would create an injection circular dependency

        Concept userConcept = ontologyRepository.getOrCreateConcept(null, LUMIFY_USER_CONCEPT_ID, "lumifyUser");
        userConceptId = userConcept.getId();

        Set<String> authorizationsSet = new HashSet<String>();
        authorizationsSet.add(VISIBILITY_STRING);
        this.authorizations = authorizationBuilder.create(authorizationsSet);
    }

    public Vertex findByUserName(String username) {
        return Iterables.getFirst(graph.query(authorizations)
                .has(USERNAME.getKey(), username)
                .has(CONCEPT_TYPE.getKey(), userConceptId)
                .vertices(), null);
    }

    public Iterable<Vertex> findAll() {
        return graph.query(authorizations)
                .has(CONCEPT_TYPE.getKey(), userConceptId)
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
        VertexBuilder userBuilder = graph.prepareVertex(userId, VISIBILITY, this.authorizations);
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

    public Vertex setCurrentWorkspace(String userId, String workspaceId) {
        Vertex user = findById(userId);
        if (user == null) {
            throw new RuntimeException("Could not find user: " + userId);
        }
        CURRENT_WORKSPACE.setProperty(user, workspaceId, VISIBILITY);
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

    public void addAuthorization(Vertex userVertex, String auth) {
        Set<String> authorizations = UserLumifyProperties.getAuthorizations(userVertex);
        if (authorizations.contains(auth)) {
            return;
        }
        authorizations.add(auth);

        addAuthorizationToGraph(auth);

        String authorizationsString = StringUtils.join(authorizations, ",");
        AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY);
        graph.flush();
    }

    public void removeAuthorization(Vertex userVertex, String auth) {
        Set<String> authorizations = UserLumifyProperties.getAuthorizations(userVertex);
        if (!authorizations.contains(auth)) {
            return;
        }
        authorizations.remove(auth);
        String authorizationsString = StringUtils.join(authorizations, ",");
        AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY);
        graph.flush();
    }

    public void addAuthorizationToGraph(String auth) {
        // TODO this code should be moved out of UserRepository
        // TODO this code is not safe across a cluster since it is not atomic. One possibility is to create a table of authorizations and always read/write from that table.
        synchronized (graph) {
            if (graph instanceof AccumuloGraph) {
                try {
                    AccumuloGraph accumuloGraph = (AccumuloGraph) graph;
                    String principal = accumuloGraph.getConnector().whoami();
                    Authorizations currentAuthorizations = accumuloGraph.getConnector().securityOperations().getUserAuthorizations(principal);
                    if (currentAuthorizations.contains(auth)) {
                        return;
                    }
                    List<byte[]> newAuthorizationsArray = new ArrayList<byte[]>();
                    for (byte[] currentAuth : currentAuthorizations) {
                        newAuthorizationsArray.add(currentAuth);
                    }
                    newAuthorizationsArray.add(auth.getBytes(Constants.UTF8));
                    Authorizations newAuthorizations = new Authorizations(newAuthorizationsArray);
                    accumuloGraph.getConnector().securityOperations().changeUserAuthorizations(principal, newAuthorizations);
                } catch (Exception ex) {
                    throw new RuntimeException("Could not update authorizations in accumulo", ex);
                }
            } else {
                throw new RuntimeException("graph type not supported to add authorizations.");
            }
        }
    }

    public void removeAuthorizationFromGraph(String auth) {
        // TODO this code should be moved out of UserRepository
        // TODO this code is not safe across a cluster since it is not atomic. One possibility is to create a table of authorizations and always read/write from that table.
        synchronized (graph) {
            if (graph instanceof AccumuloGraph) {
                try {
                    AccumuloGraph accumuloGraph = (AccumuloGraph) graph;
                    String principal = accumuloGraph.getConnector().whoami();
                    Authorizations currentAuthorizations = accumuloGraph.getConnector().securityOperations().getUserAuthorizations(principal);
                    if (!currentAuthorizations.contains(auth)) {
                        return;
                    }
                    byte[] authBytes = auth.getBytes(Constants.UTF8);
                    List<byte[]> newAuthorizationsArray = new ArrayList<byte[]>();
                    for (byte[] currentAuth : currentAuthorizations) {
                        if (Arrays.equals(currentAuth, authBytes)) {
                            continue;
                        }
                        newAuthorizationsArray.add(currentAuth);
                    }
                    Authorizations newAuthorizations = new Authorizations(newAuthorizationsArray);
                    accumuloGraph.getConnector().securityOperations().changeUserAuthorizations(principal, newAuthorizations);
                } catch (Exception ex) {
                    throw new RuntimeException("Could not update authorizations in accumulo", ex);
                }
            } else {
                throw new RuntimeException("graph type not supported to add authorizations.");
            }
        }
    }

    public com.altamiracorp.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations) {
        Vertex userVertex = findById(user.getUserId());
        Set<String> authorizationsSet = UserLumifyProperties.getAuthorizations(userVertex);
        Collections.addAll(authorizationsSet, additionalAuthorizations);
        return authorizationBuilder.create(authorizationsSet);
    }
}
