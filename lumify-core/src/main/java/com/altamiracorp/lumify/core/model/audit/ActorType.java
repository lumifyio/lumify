package com.altamiracorp.lumify.core.model.audit;

public enum ActorType {
    USER("user"),
    SYSTEM("system");

    private final String type;

    ActorType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }
}

