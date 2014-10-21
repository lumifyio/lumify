package io.lumify.core.model.user;

import org.securegraph.Authorizations;

import java.util.List;
import java.util.Set;

public interface AuthorizationRepository {

    void addAuthorizationToGraph(final String auth);

    void removeAuthorizationFromGraph(final String auth);

    List<String> getGraphAuthorizations();

    Authorizations createAuthorizations(Set<String> authorizationsSet);

    Authorizations createAuthorizations(String[] authorizations);

    Authorizations createAuthorizations(Authorizations authorizations, String... additionalAuthorizations);
}
