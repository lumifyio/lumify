package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.inmemory.InMemoryAuthorizations;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemoryAuthorizationRepository implements AuthorizationRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(InMemoryAuthorizationRepository.class);
    private List<String> authorizations = new ArrayList<String>();

    @Override
    public void init(Map config) {

    }

    @Override
    public void addAuthorizationToGraph(String auth) {
        LOGGER.info("Adding authorization to graph user %s", auth);
        authorizations.add(auth);
    }

    @Override
    public void removeAuthorizationFromGraph(String auth) {
        LOGGER.info("Removing authorization to graph user %s", auth);
        authorizations.remove(auth);
    }

    @Override
    public List<String> getGraphAuthorizations() {
        LOGGER.info("getting authorizations");
        return new ArrayList<String>(authorizations);
    }

    @Override
    public Authorizations createAuthorizations(Set<String> authorizationsSet) {
        return new InMemoryAuthorizations(Iterables.toArray(authorizationsSet, String.class));
    }
}
