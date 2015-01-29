package io.lumify.core.model.user;

import org.securegraph.Authorizations;
import org.securegraph.Visibility;

import java.util.*;

import static org.securegraph.util.IterableUtils.toArray;

public class NoOpAuthorizationRepository implements AuthorizationRepository {
    @Override
    public void addAuthorizationToGraph(String... auths) {
        // do nothing
    }

    @Override
    public void removeAuthorizationFromGraph(String auth) {
        // do nothing
    }

    @Override
    public List<String> getGraphAuthorizations() {
        return new ArrayList<String>();
    }

    @Override
    public Authorizations createAuthorizations(Set<String> authorizationsSet) {
        return createAuthorizations(toArray(authorizationsSet, String.class));
    }

    @Override
    public Authorizations createAuthorizations(String[] authorizations) {
        return new Authorizations() {
            @Override
            public boolean canRead(Visibility visibility) {
                return true;
            }

            @Override
            public String[] getAuthorizations() {
                return new String[0];
            }

            @Override
            public boolean equals(Authorizations authorizations) {
                return authorizations.getAuthorizations().length == 0;
            }
        };
    }

    @Override
    public Authorizations createAuthorizations(Authorizations authorizations, String... additionalAuthorizations) {
        Set<String> authList = new HashSet<String>();
        Collections.addAll(authList, authorizations.getAuthorizations());
        Collections.addAll(authList, additionalAuthorizations);
        return createAuthorizations(authList);
    }
}
