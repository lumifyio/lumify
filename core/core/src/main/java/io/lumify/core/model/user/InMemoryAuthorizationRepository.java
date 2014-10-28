package io.lumify.core.model.user;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Authorizations;
import org.securegraph.inmemory.InMemoryAuthorizations;

import java.util.*;

import static org.securegraph.util.IterableUtils.toArray;

public class InMemoryAuthorizationRepository implements AuthorizationRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(InMemoryAuthorizationRepository.class);
    private List<String> authorizations = new ArrayList<String>();

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
        return createAuthorizations(toArray(authorizationsSet, String.class));
    }

    @Override
    public Authorizations createAuthorizations(String[] authorizations) {
        return new InMemoryAuthorizations(authorizations);
    }

    @Override
    public Authorizations createAuthorizations(Authorizations authorizations, String... additionalAuthorizations) {
        Set<String> authList = new HashSet<String>();
        Collections.addAll(authList, authorizations.getAuthorizations());
        Collections.addAll(authList, additionalAuthorizations);
        return createAuthorizations(authList);
    }
}
