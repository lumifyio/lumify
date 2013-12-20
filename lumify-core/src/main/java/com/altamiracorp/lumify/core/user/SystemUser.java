package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SystemUser extends User {
    private static final String ROW_KEY = "system";
    private static final String USERNAME = "system";
    private static final String CURRENT_WORKSPACE = null;

    @Inject
    public SystemUser() {
        super(ROW_KEY, USERNAME, CURRENT_WORKSPACE, getSystemUserContext());
    }

    public static ModelUserContext getSystemUserContext() {
        // TODO: figure out a better way to create this
        String className = "com.altamiracorp.bigtable.model.user.accumulo.AccumuloUserContext";
        try {
            return (ModelUserContext) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create: " + className);
        }
    }
}
