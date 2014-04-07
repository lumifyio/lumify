package com.altamiracorp.lumify.core.model.ontology;

public enum PropertyType {
    DATE("date"),
    STRING("string"),
    GEO_LOCATION("geoLocation"),
    IMAGE("image"),
    CURRENCY("currency"),
    DOUBLE("double"),
    BOOLEAN("boolean");

    private final String text;

    PropertyType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public static PropertyType convert(String property) {
        for (PropertyType pt : PropertyType.values()) {
            if (pt.toString().equalsIgnoreCase(property)) {
                return pt;
            }
        }
        return STRING;
    }
}
