package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.securegraph.Vertex;

public interface UserProvider {
    User createFromVertex(Vertex user);

    User getSystemUser();

    ModelUserContext getModelUserContext(User user, String... additionalAuthorizations);
}
