package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.securegraph.accumulo.AccumuloAuthorizations;
import com.google.inject.Inject;

public class OntologyUser extends User {
    private static final String USERNAME = "ontology";
    private static final String CURRENT_WORKSPACE = null;

    @Inject
    public OntologyUser() {
        // TODO make authorizations configurable
        super("", USERNAME, CURRENT_WORKSPACE, getSystemUserContext(), UserType.SYSTEM.toString(), new AccumuloAuthorizations("ontology"));
    }

    public static ModelUserContext getSystemUserContext() {
        // TODO: figure out a better way to create this
        String className = "com.altamiracorp.bigtable.model.user.accumulo.AccumuloUserContext";
        try {
            return (ModelUserContext) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create: " + className, e);
        }
    }
}
