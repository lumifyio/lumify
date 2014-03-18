package com.altamiracorp.lumify.core.model.audit;

public enum  AuditAction {
    UPDATE("update"),
    DELETE("delete"),
    CREATE("create"),
    PUBLISH("publish");

    private final String action;

    AuditAction (String action) {
        this.action = action;
    }

    @Override
    public final String toString () {
        return this.action;
    }
}
