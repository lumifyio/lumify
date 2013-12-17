package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserType;

public class SystemUser extends User {
    private static final String USERNAME = "system";
    private static final String CURRENT_WORKSPACE = null;

    public SystemUser() {
        super("", USERNAME, CURRENT_WORKSPACE, getSystemUserContext(), UserType.SYSTEM.toString());
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
