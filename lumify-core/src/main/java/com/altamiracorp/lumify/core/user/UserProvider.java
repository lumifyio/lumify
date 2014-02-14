package com.altamiracorp.lumify.core.user;

import com.altamiracorp.securegraph.Vertex;

public interface UserProvider {
    User createFromVertex(Vertex user);

    User getSystemUser();

    User getOntologyUser();

    User getUserManagerUser();
}
