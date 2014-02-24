package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.inmemory.InMemoryAuthorizations;
import com.google.common.collect.Iterables;

import java.util.Set;

public class InMemoryAuthorizationRepository implements AuthorizationRepository {
    @Override
    public void addAuthorizationToGraph(String auth) {

    }

    @Override
    public void removeAuthorizationFromGraph(String auth) {

    }

    @Override
    public Authorizations createAuthorizations(Set<String> authorizationsSet) {
        return new InMemoryAuthorizations(Iterables.toArray(authorizationsSet, String.class));
    }
}
