package io.lumify.core.model.user;

import java.util.List;
import java.util.Set;

public interface AuthorizationRepository {

    void addAuthorizationToGraph(final String auth);

    void removeAuthorizationFromGraph(final String auth);

    List<String> getGraphAuthorizations();

    org.securegraph.Authorizations createAuthorizations(Set<String> authorizationsSet);
}
