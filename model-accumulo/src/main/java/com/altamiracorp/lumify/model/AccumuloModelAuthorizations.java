package com.altamiracorp.lumify.model;

import com.altamiracorp.lumify.core.user.ModelAuthorizations;
import org.apache.accumulo.core.security.Authorizations;

public class AccumuloModelAuthorizations extends ModelAuthorizations {
    public Authorizations getAuthorizations() {
        return new Authorizations(); // TODO: fill this out
    }
}
