package com.altamiracorp.lumify.core.user;

import com.altamiracorp.securegraph.Authorizations;

public interface AuthorizationBuilder {
    Authorizations create(String[] authorizationsArray);
}
