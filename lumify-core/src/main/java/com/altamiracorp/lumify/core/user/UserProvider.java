package com.altamiracorp.lumify.core.user;

import com.altamiracorp.lumify.core.model.user.UserRow;

public interface UserProvider {
    User createFromModelUser(UserRow user);

    User getSystemUser();

    User getOntologyUser();
}
