package com.altamiracorp.lumify.core.user;

import com.altamiracorp.securegraph.Authorizations;

import java.io.Serializable;
import java.util.Set;

public interface AuthorizationBuilder extends Serializable {
    static final long serialVersionUID = 1L;

    Authorizations create(Set<String> authorizationsArray);
}
