package io.lumify.core.model.user;

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

