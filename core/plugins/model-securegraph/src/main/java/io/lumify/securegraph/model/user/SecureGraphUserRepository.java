package io.lumify.securegraph.model.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.*;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.web.clientapi.model.Privilege;
import io.lumify.core.user.SystemUser;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.UserStatus;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.util.ConvertingIterable;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.singleOrDefault;
import static org.securegraph.util.IterableUtils.toArray;

@Singleton
public class SecureGraphUserRepository extends UserRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SecureGraphUserRepository.class);
    private final AuthorizationRepository authorizationRepository;
    private Graph graph;
    private String userConceptId;
    private org.securegraph.Authorizations authorizations;
    private final Cache<String, Set<String>> userAuthorizationCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();
    private final Cache<String, Set<Privilege>> userPrivilegesCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();
    private UserListenerUtil userListenerUtil;

    @Inject
    public SecureGraphUserRepository(
            final Configuration configuration,
            final AuthorizationRepository authorizationRepository,
            final Graph graph,
            final OntologyRepository ontologyRepository,
            final UserListenerUtil userListenerUtil) {
        super(configuration);
        this.authorizationRepository = authorizationRepository;
        this.graph = graph;
        this.userListenerUtil = userListenerUtil;

        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        authorizationRepository.addAuthorizationToGraph(LumifyVisibility.SUPER_USER_VISIBILITY_STRING);

        Concept userConcept = ontologyRepository.getOrCreateConcept(null, USER_CONCEPT_IRI, "lumifyUser", null);
        userConceptId = userConcept.getIRI();

        Set<String> authorizationsSet = new HashSet<>();
        authorizationsSet.add(VISIBILITY_STRING);
        authorizationsSet.add(LumifyVisibility.SUPER_USER_VISIBILITY_STRING);
        this.authorizations = authorizationRepository.createAuthorizations(authorizationsSet);
    }

    private SecureGraphUser createFromVertex(Vertex user) {
        if (user == null) {
            return null;
        }

        String userId = user.getId();
        String username = UserLumifyProperties.USERNAME.getPropertyValue(user);
        String displayName = UserLumifyProperties.DISPLAY_NAME.getPropertyValue(user);
        String emailAddress = UserLumifyProperties.EMAIL_ADDRESS.getPropertyValue(user);
        Date createDate = UserLumifyProperties.CREATE_DATE.getPropertyValue(user);
        Date currentLoginDate = UserLumifyProperties.CURRENT_LOGIN_DATE.getPropertyValue(user);
        String currentLoginRemoteAddr = UserLumifyProperties.CURRENT_LOGIN_REMOTE_ADDR.getPropertyValue(user);
        Date previousLoginDate = UserLumifyProperties.PREVIOUS_LOGIN_DATE.getPropertyValue(user);
        String previousLoginRemoteAddr = UserLumifyProperties.PREVIOUS_LOGIN_REMOTE_ADDR.getPropertyValue(user);
        int loginCount = UserLumifyProperties.LOGIN_COUNT.getPropertyValue(user, 0);
        String[] authorizations = toArray(getAuthorizations(user), String.class);
        ModelUserContext modelUserContext = getModelUserContext(authorizations);
        UserStatus userStatus = UserStatus.valueOf(UserLumifyProperties.STATUS.getPropertyValue(user));
        Set<Privilege> privileges = Privilege.stringToPrivileges(UserLumifyProperties.PRIVILEGES.getPropertyValue(user));
        String currentWorkspaceId = UserLumifyProperties.CURRENT_WORKSPACE.getPropertyValue(user);
        JSONObject preferences = UserLumifyProperties.UI_PREFERENCES.getPropertyValue(user);
        String passwordResetToken = UserLumifyProperties.PASSWORD_RESET_TOKEN.getPropertyValue(user);
        Date passwordResetTokenExpirationDate = UserLumifyProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.getPropertyValue(user);

        LOGGER.debug("Creating user from UserRow. username: %s", username);
        return new SecureGraphUser(userId, username, displayName, emailAddress, createDate, currentLoginDate, currentLoginRemoteAddr, previousLoginDate, previousLoginRemoteAddr, loginCount, modelUserContext, userStatus, privileges, currentWorkspaceId, preferences, passwordResetToken, passwordResetTokenExpirationDate);
    }

    @Override
    public User findByUsername(String username) {
        username = formatUsername(username);
        return createFromVertex(singleOrDefault(graph.query(authorizations)
                .has(UserLumifyProperties.USERNAME.getPropertyName(), username)
                .has(LumifyProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .vertices(), null));
    }

    @Override
    public Iterable<User> find(int skip, int limit) {
        return new ConvertingIterable<Vertex, User>(graph.query(authorizations)
                .has(LumifyProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .skip(skip)
                .limit(limit)
                .vertices()) {
            @Override
            protected User convert(Vertex vertex) {
                return createFromVertex(vertex);
            }
        };
    }

    @Override
    public Iterable<User> findByStatus(int skip, int limit, UserStatus status) {
        return new ConvertingIterable<Vertex, User>(graph.query(authorizations)
                .has(LumifyProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .has(UserLumifyProperties.STATUS.getPropertyName(), status.toString())
                .skip(skip)
                .limit(limit)
                .vertices()) {
            @Override
            protected User convert(Vertex vertex) {
                return createFromVertex(vertex);
            }
        };
    }

    @Override
    public User findById(String userId) {
        return createFromVertex(findByIdUserVertex(userId));
    }

    public Vertex findByIdUserVertex(String userId) {
        return graph.getVertex(userId, authorizations);
    }

    @Override
    public User addUser(String username, String displayName, String emailAddress, String password, String[] userAuthorizations) {
        username = formatUsername(username);
        displayName = displayName.trim();
        User existingUser = findByUsername(username);
        if (existingUser != null) {
            throw new LumifyException("duplicate username");
        }

        String authorizationsString = StringUtils.join(userAuthorizations, ",");

        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

        String id = "USER_" + graph.getIdGenerator().nextId();
        VertexBuilder userBuilder = graph.prepareVertex(id, VISIBILITY.getVisibility());

        LumifyProperties.CONCEPT_TYPE.setProperty(userBuilder, userConceptId, VISIBILITY.getVisibility());
        UserLumifyProperties.USERNAME.setProperty(userBuilder, username, VISIBILITY.getVisibility());
        UserLumifyProperties.DISPLAY_NAME.setProperty(userBuilder, displayName, VISIBILITY.getVisibility());
        UserLumifyProperties.CREATE_DATE.setProperty(userBuilder, new Date(), VISIBILITY.getVisibility());
        UserLumifyProperties.PASSWORD_SALT.setProperty(userBuilder, salt, VISIBILITY.getVisibility());
        UserLumifyProperties.PASSWORD_HASH.setProperty(userBuilder, passwordHash, VISIBILITY.getVisibility());
        UserLumifyProperties.STATUS.setProperty(userBuilder, UserStatus.OFFLINE.toString(), VISIBILITY.getVisibility());
        UserLumifyProperties.AUTHORIZATIONS.setProperty(userBuilder, authorizationsString, VISIBILITY.getVisibility());
        UserLumifyProperties.PRIVILEGES.setProperty(userBuilder, Privilege.toString(getDefaultPrivileges()), VISIBILITY.getVisibility());

        if (emailAddress != null) {
            UserLumifyProperties.EMAIL_ADDRESS.setProperty(userBuilder, emailAddress, VISIBILITY.getVisibility());
        }

        User user = createFromVertex(userBuilder.save(this.authorizations));
        graph.flush();

        userListenerUtil.fireNewUserAddedEvent(user);

        return user;
    }

    @Override
    public void setPassword(User user, String password) {
        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        UserLumifyProperties.PASSWORD_SALT.setProperty(userVertex, salt, VISIBILITY.getVisibility(), authorizations);
        UserLumifyProperties.PASSWORD_HASH.setProperty(userVertex, passwordHash, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    public boolean isPasswordValid(User user, String password) {
        try {
            Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
            return UserPasswordUtil.validatePassword(password, UserLumifyProperties.PASSWORD_SALT.getPropertyValue(userVertex), UserLumifyProperties.PASSWORD_HASH.getPropertyValue(userVertex));
        } catch (Exception ex) {
            throw new RuntimeException("error validating password", ex);
        }
    }

    @Override
    public void recordLogin(User user, String remoteAddr) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);

        Date currentLoginDate = UserLumifyProperties.CURRENT_LOGIN_DATE.getPropertyValue(userVertex);
        if (currentLoginDate != null) {
            UserLumifyProperties.PREVIOUS_LOGIN_DATE.setProperty(userVertex, currentLoginDate, VISIBILITY.getVisibility(), authorizations);
        }

        String currentLoginRemoteAddr = UserLumifyProperties.CURRENT_LOGIN_REMOTE_ADDR.getPropertyValue(userVertex);
        if (currentLoginRemoteAddr != null) {
            UserLumifyProperties.PREVIOUS_LOGIN_REMOTE_ADDR.setProperty(userVertex, currentLoginRemoteAddr, VISIBILITY.getVisibility(), authorizations);
        }

        UserLumifyProperties.CURRENT_LOGIN_DATE.setProperty(userVertex, new Date(), VISIBILITY.getVisibility(), authorizations);
        UserLumifyProperties.CURRENT_LOGIN_REMOTE_ADDR.setProperty(userVertex, remoteAddr, VISIBILITY.getVisibility(), authorizations);

        int loginCount = UserLumifyProperties.LOGIN_COUNT.getPropertyValue(userVertex, 0);
        UserLumifyProperties.LOGIN_COUNT.setProperty(userVertex, loginCount + 1, VISIBILITY.getVisibility(), authorizations);

        graph.flush();
    }

    @Override
    public User setCurrentWorkspace(String userId, String workspaceId) {
        User user = findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        UserLumifyProperties.CURRENT_WORKSPACE.setProperty(userVertex, workspaceId, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        return user;
    }

    @Override
    public String getCurrentWorkspaceId(String userId) {
        User user = findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        return UserLumifyProperties.CURRENT_WORKSPACE.getPropertyValue(userVertex);
    }

    @Override
    public void setUiPreferences(User user, JSONObject preferences) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        UserLumifyProperties.UI_PREFERENCES.setProperty(userVertex, preferences, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    public User setStatus(String userId, UserStatus status) {
        SecureGraphUser user = (SecureGraphUser) findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        UserLumifyProperties.STATUS.setProperty(userVertex, status.toString(), VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        user.setUserStatus(status);
        return user;
    }

    @Override
    public void addAuthorization(User user, String auth) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        Set<String> authorizationSet = getAuthorizations(userVertex);
        if (authorizationSet.contains(auth)) {
            return;
        }
        authorizationSet.add(auth);

        this.authorizationRepository.addAuthorizationToGraph(auth);

        String authorizationsString = StringUtils.join(authorizationSet, ",");
        UserLumifyProperties.AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        userAuthorizationCache.invalidate(user.getUserId());
    }

    @Override
    public void removeAuthorization(User user, String auth) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        Set<String> authorizationSet = getAuthorizations(userVertex);
        if (!authorizationSet.contains(auth)) {
            return;
        }
        authorizationSet.remove(auth);
        String authorizationsString = StringUtils.join(authorizationSet, ",");
        UserLumifyProperties.AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        userAuthorizationCache.invalidate(user.getUserId());
    }

    @Override
    public org.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations) {
        Set<String> userAuthorizations;
        if (user instanceof SystemUser) {
            userAuthorizations = new HashSet<String>();
            userAuthorizations.add(LumifyVisibility.SUPER_USER_VISIBILITY_STRING);
        } else {
            userAuthorizations = userAuthorizationCache.getIfPresent(user.getUserId());
        }
        if (userAuthorizations == null) {
            Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
            userAuthorizations = getAuthorizations(userVertex);
            userAuthorizationCache.put(user.getUserId(), userAuthorizations);
        }

        Set<String> authorizationsSet = new HashSet<String>(userAuthorizations);
        Collections.addAll(authorizationsSet, additionalAuthorizations);
        return authorizationRepository.createAuthorizations(authorizationsSet);
    }

    public static Set<String> getAuthorizations(Vertex userVertex) {
        String authorizationsString = UserLumifyProperties.AUTHORIZATIONS.getPropertyValue(userVertex);
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

    @Override
    public void setDisplayName(User user, String displayName) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        UserLumifyProperties.DISPLAY_NAME.setProperty(userVertex, displayName, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    public void setEmailAddress(User user, String emailAddress) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        UserLumifyProperties.EMAIL_ADDRESS.setProperty(userVertex, emailAddress, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    public Set<Privilege> getPrivileges(User user) {
        Set<Privilege> privileges;
        if (user instanceof SystemUser) {
            return Privilege.ALL;
        } else {
            privileges = userPrivilegesCache.getIfPresent(user.getUserId());
        }
        if (privileges == null) {
            Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
            privileges = getPrivileges(userVertex);
            userPrivilegesCache.put(user.getUserId(), privileges);
        }
        return privileges;
    }

    @Override
    public void delete(User user) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        graph.removeVertex(userVertex, authorizations);
    }

    @Override
    public void setPrivileges(User user, Set<Privilege> privileges) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserLumifyProperties.PRIVILEGES.setProperty(userVertex, Privilege.toString(privileges), VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        userPrivilegesCache.invalidate(user.getUserId());
    }

    private Set<Privilege> getPrivileges(Vertex userVertex) {
        return Privilege.stringToPrivileges(UserLumifyProperties.PRIVILEGES.getPropertyValue(userVertex));
    }

    @Override
    public User findByPasswordResetToken(String token) {
        return createFromVertex(singleOrDefault(graph.query(authorizations)
                .has(UserLumifyProperties.PASSWORD_RESET_TOKEN.getPropertyName(), token)
                .has(LumifyProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .vertices(), null));
    }

    @Override
    public void setPasswordResetTokenAndExpirationDate(User user, String token, Date expirationDate) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserLumifyProperties.PASSWORD_RESET_TOKEN.setProperty(userVertex, token, VISIBILITY.getVisibility(), authorizations);
        UserLumifyProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.setProperty(userVertex, expirationDate, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    public void clearPasswordResetTokenAndExpirationDate(User user) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserLumifyProperties.PASSWORD_RESET_TOKEN.removeProperty(userVertex, authorizations);
        UserLumifyProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.removeProperty(userVertex, authorizations);
        graph.flush();
    }
}
