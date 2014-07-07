package io.lumify.core.model.audit;

public enum  AuditAction {
    UPDATE("update"),
    DELETE("delete"),
    CREATE("create"),
    PUBLISH("publish"),
    UNRESOLVE("unresolved"),
    ANALYZED_BY("analyzed by");

    private final String action;

    AuditAction (String action) {
        this.action = action;
    }

    @Override
    public final String toString () {
        return this.action;
    }
}
