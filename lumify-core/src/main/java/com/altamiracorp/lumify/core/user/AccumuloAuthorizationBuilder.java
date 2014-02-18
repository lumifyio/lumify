package com.altamiracorp.lumify.core.user;

import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.accumulo.AccumuloAuthorizations;
import com.google.common.collect.Iterables;

import java.io.Serializable;
import java.util.Set;

public class AccumuloAuthorizationBuilder implements AuthorizationBuilder, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public Authorizations create(Set<String> authorizations) {
        return new AccumuloAuthorizations(Iterables.toArray(authorizations, String.class));
    }
}
