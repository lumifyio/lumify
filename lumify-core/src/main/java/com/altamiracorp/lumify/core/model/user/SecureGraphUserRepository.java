package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.SecureGraphUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.VertexBuilder;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.*;

public class SecureGraphUserRepository extends UserRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SecureGraphUserRepository.class);
    private Graph graph;
    private String userConceptId;
    private com.altamiracorp.securegraph.Authorizations authorizations;
    private AuthorizationRepository authorizationRepository;

    public SecureGraphUserRepository (final Graph graph,
                                      final OntologyRepository ontologyRepository,
                                      final AuthorizationRepository authorizationRepository) {
        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        authorizationRepository.addAuthorizationToGraph(LumifyVisibility.VISIBILITY_STRING);

        Concept userConcept = ontologyRepository.getOrCreateConcept(null, LUMIFY_USER_CONCEPT_ID, "lumifyUser");
        userConceptId = userConcept.getId();

        Set<String> authorizationsSet = new HashSet<String>();
        authorizationsSet.add(VISIBILITY_STRING);
        authorizationsSet.add(LumifyVisibility.VISIBILITY_STRING);
        this.authorizations = authorizationRepository.createAuthorizations(authorizationsSet);
        this.authorizationRepository = authorizationRepository;
        this.graph = graph;
    }

    private SecureGraphUser createFromVertex(Vertex user) {
        if (user == null) {
            return null;
        }

        String[] authorizations = Iterables.toArray(getAuthorizations(user), String.class);
        ModelUserContext modelUserContext = getModelUserContext(authorizations);

        LOGGER.debug("Creating user from UserRow. userName: %s, authorizations: %s", USERNAME.getPropertyValue(user), AUTHORIZATIONS.getPropertyValue(user));
        return new SecureGraphUser(user, modelUserContext);
    }

    @Override
    public User findByDisplayName(String username) {
        return createFromVertex(Iterables.getFirst(graph.query(authorizations)
                .has(USERNAME.getKey(), username)
                .has(CONCEPT_TYPE.getKey(), userConceptId)
                .vertices(), null));
    }

    @Override
    public Iterable<User> findAll() {
        return new ConvertingIterable<Vertex, User>(graph.query(authorizations)
                .has(CONCEPT_TYPE.getKey(), userConceptId)
                .vertices()) {
            @Override
            protected User convert(Vertex vertex) {
                return createFromVertex(vertex);
            }
        };
    }

    @Override
    public User findById(String userId) {
        return createFromVertex(graph.getVertex(userId, authorizations));
    }

    @Override
    public User addUser(String userId, String displayName, String password, String[] userAuthorizations) {
        User existingUser = findByDisplayName(displayName);
        if (existingUser != null) {
            throw new RuntimeException("");
        }

        String authorizationsString = StringUtils.join(userAuthorizations, ",");

        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

        userId = "USER_" + userId;
        VertexBuilder userBuilder = graph.prepareVertex(userId, VISIBILITY.getVisibility(), this.authorizations);
        USERNAME.setProperty(userBuilder, displayName, VISIBILITY.getVisibility());
        CONCEPT_TYPE.setProperty(userBuilder, userConceptId, VISIBILITY.getVisibility());
        PASSWORD_SALT.setProperty(userBuilder, salt, VISIBILITY.getVisibility());
        PASSWORD_HASH.setProperty(userBuilder, passwordHash, VISIBILITY.getVisibility());
        STATUS.setProperty(userBuilder, UserStatus.OFFLINE.toString(), VISIBILITY.getVisibility());
        AUTHORIZATIONS.setProperty(userBuilder, authorizationsString, VISIBILITY.getVisibility());
        User user = createFromVertex(userBuilder.save());
        graph.flush();
        return user;
    }

    @Override
    public void setPassword(User user, String password) {
        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        PASSWORD_SALT.setProperty(userVertex, salt, VISIBILITY.getVisibility());
        PASSWORD_HASH.setProperty(userVertex, passwordHash, VISIBILITY.getVisibility());
        graph.flush();
    }

    @Override
    public boolean isPasswordValid(User user, String password) {
        try {
            Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
            return UserPasswordUtil.validatePassword(password, PASSWORD_SALT.getPropertyValue(userVertex), PASSWORD_HASH.getPropertyValue(userVertex));
        } catch (Exception ex) {
            throw new RuntimeException("error validating password", ex);
        }
    }

    @Override
    public User setCurrentWorkspace(String userId, String workspaceId) {
        User user = findById(userId);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        if (user == null) {
            throw new RuntimeException("Could not find user: " + userId);
        }
        CURRENT_WORKSPACE.setProperty(userVertex, workspaceId, VISIBILITY.getVisibility());
        graph.flush();
        return user;
    }

    @Override
    public User setStatus(String userId, UserStatus status) {
        User user = findById(userId);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        if (user == null) {
            throw new RuntimeException("Could not find user: " + userId);
        }
        STATUS.setProperty(userVertex, status.toString(), VISIBILITY.getVisibility());
        graph.flush();
        return user;
    }

    @Override
    public void addAuthorization(User user, String auth) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        Set<String> authorizations = getAuthorizations(userVertex);
        if (authorizations.contains(auth)) {
            return;
        }
        authorizations.add(auth);

        this.authorizationRepository.addAuthorizationToGraph(auth);

        String authorizationsString = StringUtils.join(authorizations, ",");
        AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY.getVisibility());
        graph.flush();
    }

    @Override
    public void removeAuthorization(User user, String auth) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        Set<String> authorizations = getAuthorizations(userVertex);
        if (!authorizations.contains(auth)) {
            return;
        }
        authorizations.remove(auth);
        String authorizationsString = StringUtils.join(authorizations, ",");
        AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY.getVisibility());
        graph.flush();
    }

    @Override
    public com.altamiracorp.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        Set<String> authorizationsSet = getAuthorizations(userVertex);
        Collections.addAll(authorizationsSet, additionalAuthorizations);
        return authorizationRepository.createAuthorizations(authorizationsSet);
    }

    public static Set<String> getAuthorizations(Vertex userVertex) {
        String authorizationsString = AUTHORIZATIONS.getPropertyValue(userVertex);
        if (authorizationsString == null) {
            return new HashSet<String>();
        }
        String[] authorizationsArray = authorizationsString.split(",");
        if (authorizationsArray.length == 1 && authorizationsArray[0].length() == 0) {
            authorizationsArray = new String[0];
        }
        HashSet<String> authorizations = new HashSet<String>();
        for (String s : authorizationsArray) {
            // Accumulo doesn't like zero length strings. they shouldn't be in the auth string to begin with but this just protects from that happening.
            if (s.trim().length() == 0) {
                continue;
            }

            authorizations.add(s);
        }
        return authorizations;
    }
}
