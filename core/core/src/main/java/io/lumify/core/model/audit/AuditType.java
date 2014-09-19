package io.lumify.core.model.audit;

public enum AuditType {
    PROPERTY ("property"),
    RELATIONSHIP ("relationship"),
    ENTITY ("entity");

    private final String type;

    AuditType (String type) {
        this.type = type;
    }

    @Override
    public final String toString () {
        return this.type;
    }
}