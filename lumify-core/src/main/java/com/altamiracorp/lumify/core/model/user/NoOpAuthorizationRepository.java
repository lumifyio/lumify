package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Visibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NoOpAuthorizationRepository implements AuthorizationRepository {
    @Override
    public void addAuthorizationToGraph(String auth) {
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
        return new Authorizations() {
            @Override
            public boolean canRead(Visibility visibility) {
                return true;
            }

            @Override
            public String[] getAuthorizations() {
                return new String[0];
            }
        };
    }
}
