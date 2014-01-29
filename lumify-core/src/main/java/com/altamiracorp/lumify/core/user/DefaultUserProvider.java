package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.securegraph.accumulo.AccumuloAuthorizations;

public class DefaultUserProvider implements UserProvider {
    private static final String ONTOLOGY_USERNAME = "ontology";
    private static final String SYSTEM_USERNAME = "system";
    private User systemUser;
    private User ontologyUser;

    public User createFromModelUser(UserRow user) {
        // TODO change to user specific authorization
        String[] authorizations = new String[0];
        ModelUserContext modelUserContext = getSystemUserContext(authorizations);

        return new User(
                user.getRowKey().toString(),
                user.getMetadata().getUserName(),
                user.getMetadata().getCurrentWorkspace(),
                modelUserContext,
                user.getMetadata().getUserType(),
                new AccumuloAuthorizations(authorizations));
    }

    @Override
    public User getSystemUser() {
        if (systemUser == null) {
            String workspace = null;
            String rowKey = "";
            systemUser = new User(rowKey, SYSTEM_USERNAME, workspace, getSystemUserContext(), UserType.SYSTEM.toString(), new AccumuloAuthorizations());
        }
        return systemUser;
    }

    @Override
    public User getOntologyUser() {
        if (ontologyUser == null) {
            String workspace = null;
            String rowKey = "";
            ontologyUser = new User(rowKey, ONTOLOGY_USERNAME, workspace, getSystemUserContext(), UserType.SYSTEM.toString(), new AccumuloAuthorizations("ontology"));
        }
        return ontologyUser;
    }

    private static ModelUserContext getSystemUserContext(String... authorizations) {
        // TODO: figure out a better way to create this
        String className = "com.altamiracorp.bigtable.model.user.accumulo.AccumuloUserContext";
        try {
            return (ModelUserContext) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create: " + className, e);
        }
    }
}
