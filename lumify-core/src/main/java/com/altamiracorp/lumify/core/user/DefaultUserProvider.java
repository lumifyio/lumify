package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.bigtable.model.user.accumulo.AccumuloUserContext;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.accumulo.AccumuloAuthorizations;
import org.apache.accumulo.core.security.Authorizations;

public class DefaultUserProvider implements UserProvider {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DefaultUserProvider.class);
    private static final String ONTOLOGY_USERNAME = "ontology";
    private static final String SYSTEM_USERNAME = "system";
    private User systemUser;
    private User ontologyUser;

    public User createFromModelUser(UserRow user) {
        String[] authorizations = user.getMetadata().getAuthorizationsAsArray();
        ModelUserContext modelUserContext = getSystemUserContext(authorizations);

        LOGGER.debug("Creating user from UserRow. userName: %s, authorizations: %s", user.getMetadata().getUserName(), user.getMetadata().getAuthorizations());
        return new User(
                user.getRowKey().toString(),
                user.getMetadata().getUserName(),
                user.getMetadata().getCurrentWorkspace(),
                modelUserContext,
                user.getMetadata().getUserType(),
                new AccumuloAuthorizations(authorizations));
    }

    @Override
    public User getSystemUser() {
        if (systemUser == null) {
            String workspace = null;
            String rowKey = "";
            systemUser = new User(rowKey, SYSTEM_USERNAME, workspace, getSystemUserContext(), UserType.SYSTEM.toString(), new AccumuloAuthorizations());
        }
        return systemUser;
    }

    @Override
    public User getOntologyUser() {
        if (ontologyUser == null) {
            String workspace = null;
            String rowKey = "";
            ontologyUser = new User(rowKey, ONTOLOGY_USERNAME, workspace, getSystemUserContext(), UserType.SYSTEM.toString(), new AccumuloAuthorizations("ontology"));
        }
        return ontologyUser;
    }

    private static ModelUserContext getSystemUserContext(String... authorizations) {
        // TODO: figure out a better way to create this without requiring accumulo
        return new AccumuloUserContext(new Authorizations(authorizations));
    }
}
