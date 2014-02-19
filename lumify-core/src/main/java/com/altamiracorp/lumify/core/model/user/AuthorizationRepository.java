package com.altamiracorp.lumify.core.model.user;

import java.util.Set;

public interface AuthorizationRepository {
    void addAuthorizationToGraph(final String auth);

    void removeAuthorizationFromGraph(final String auth);

    com.altamiracorp.securegraph.Authorizations createAuthorizations(Set<String> authorizationsSet);
}
