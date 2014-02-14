package com.altamiracorp.lumify.core.user;

import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.accumulo.AccumuloAuthorizations;

public class AccumuloAuthorizationBuilder implements AuthorizationBuilder {
    @Override
    public Authorizations create(String[] authorizationsArray) {
        return new AccumuloAuthorizations(authorizationsArray);
    }
}
