package io.lumify.web.clientapi.model;

public enum UserType {
    USER("user"),
    SYSTEM("system");

    private final String type;

    UserType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }
}

