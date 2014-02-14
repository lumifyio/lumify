package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.bigtable.model.user.accumulo.AccumuloUserContext;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserLumifyProperties;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Vertex;
import org.apache.accumulo.core.security.Authorizations;

import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.*;

public class DefaultUserProvider implements UserProvider {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DefaultUserProvider.class);
    private static final String ONTOLOGY_USERNAME = "ontology";
    private static final String SYSTEM_USERNAME = "system";
    private static final String USER_MANAGER_USERNAME = "userManager";
    private static final String WORKSPACE_USERNAME = "workspace";
    private User systemUser;
    private User ontologyUser;
    private User userManagerUser;
    private User workspaceUser;
    private AuthorizationBuilder authorizationBuilder = new AccumuloAuthorizationBuilder();

    public User createFromVertex(Vertex user) {
        String[] authorizations = UserLumifyProperties.getAuthorizationsArray(user);
        ModelUserContext modelUserContext = getSystemUserContext(authorizations);

        LOGGER.debug("Creating user from UserRow. userName: %s, authorizations: %s", USERNAME.getPropertyValue(user), AUTHORIZATIONS.getPropertyValue(user));
        return new User(
                user.getId().toString(),
                USERNAME.getPropertyValue(user),
                CURRENT_WORKSPACE.getPropertyValue(user),
                modelUserContext,
                UserType.USER,
                authorizationBuilder,
                authorizations);
    }

    @Override
    public User getSystemUser() {
        if (systemUser == null) {
            String workspace = null;
            String rowKey = "";
            systemUser = new User(rowKey, SYSTEM_USERNAME, workspace, getSystemUserContext(), UserType.SYSTEM, authorizationBuilder, new String[0]);
        }
        return systemUser;
    }

    @Override
    public User getOntologyUser() {
        if (ontologyUser == null) {
            String workspace = null;
            String userId = "";
            ontologyUser = new User(userId, ONTOLOGY_USERNAME, workspace, getSystemUserContext(), UserType.SYSTEM, authorizationBuilder, new String[]{OntologyRepository.VISIBILITY_STRING});
        }
        return ontologyUser;
    }

    @Override
    public User getUserManagerUser() {
        if (userManagerUser == null) {
            String workspace = null;
            String userId = "";
            userManagerUser = new User(userId, USER_MANAGER_USERNAME, workspace, getSystemUserContext(), UserType.SYSTEM, authorizationBuilder, new String[]{UserRepository.VISIBILITY_STRING});
        }
        return userManagerUser;
    }

    private static ModelUserContext getSystemUserContext(String... authorizations) {
        // TODO: figure out a better way to create this without requiring accumulo
        return new AccumuloUserContext(new Authorizations(authorizations));
    }
}
