package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.bigtable.model.user.accumulo.AccumuloUserContext;
import com.altamiracorp.lumify.core.model.user.UserLumifyProperties;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Vertex;
import com.google.common.collect.Iterables;
import org.apache.accumulo.core.security.Authorizations;

import java.util.ArrayList;

import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.*;

public class DefaultUserProvider implements UserProvider {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DefaultUserProvider.class);
    private static final String SYSTEM_USERNAME = "system";
    private User systemUser;

    public User createFromVertex(Vertex user) {
        String[] authorizations = Iterables.toArray(UserLumifyProperties.getAuthorizations(user), String.class);
        ModelUserContext modelUserContext = getModelUserContext(authorizations);

        LOGGER.debug("Creating user from UserRow. userName: %s, authorizations: %s", USERNAME.getPropertyValue(user), AUTHORIZATIONS.getPropertyValue(user));
        return new User(
                user.getId().toString(),
                USERNAME.getPropertyValue(user),
                CURRENT_WORKSPACE.getPropertyValue(user),
                modelUserContext,
                authorizations,
                UserType.USER);
    }

    @Override
    public User getSystemUser() {
        if (systemUser == null) {
            String[] authorizations = new String[0];
            String workspace = null;
            String rowKey = "";
            systemUser = new User(rowKey, SYSTEM_USERNAME, workspace, getModelUserContext(authorizations), authorizations, UserType.SYSTEM);
        }
        return systemUser;
    }

    @Override
    public ModelUserContext getModelUserContext(User user, String... additionalAuthorizations) {
        ArrayList<String> authorizations = new ArrayList<String>();

        if (user.getAuthorizations() != null) {
            for (String a : user.getAuthorizations()) {
                if (a != null && a.length() > 0) {
                    authorizations.add(a);
                }
            }
        }

        if (additionalAuthorizations != null) {
            for (String a : additionalAuthorizations) {
                if (a != null && a.length() > 0) {
                    authorizations.add(a);
                }
            }
        }

        return getModelUserContext(authorizations.toArray(new String[authorizations.size()]));
    }

    private static ModelUserContext getModelUserContext(String... authorizations) {
        // TODO: figure out a better way to create this without requiring accumulo
        return new AccumuloUserContext(new Authorizations(authorizations));
    }
}
