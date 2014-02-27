package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.inmemory.InMemoryAuthorizations;
import com.google.common.collect.Iterables;

import java.util.Set;

public class InMemoryAuthorizationRepository implements AuthorizationRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(InMemoryAuthorizationRepository.class);

    @Override
    public void addAuthorizationToGraph(String auth) {
        LOGGER.info("Adding authorization to graph user %s", auth);
    }

    @Override
    public void removeAuthorizationFromGraph(String auth) {
        LOGGER.info("Removing authorization to graph user %s", auth);
    }

    @Override
    public Authorizations createAuthorizations(Set<String> authorizationsSet) {
        return new InMemoryAuthorizations(Iterables.toArray(authorizationsSet, String.class));
    }
}
