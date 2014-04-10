package com.altamiracorp.lumify.core.model.user;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AuthorizationRepository {

    void init(Map config);

    void addAuthorizationToGraph(final String auth);

    void removeAuthorizationFromGraph(final String auth);

    List<String> getGraphAuthorizations();

    com.altamiracorp.securegraph.Authorizations createAuthorizations(Set<String> authorizationsSet);
}
