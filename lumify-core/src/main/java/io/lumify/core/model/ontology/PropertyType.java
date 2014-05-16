package io.lumify.core.model.ontology;

import io.lumify.core.exception.LumifyException;
import org.securegraph.type.GeoPoint;

import java.util.Date;

public enum PropertyType {
    DATE("date"),
    STRING("string"),
    GEO_LOCATION("geoLocation"),
    IMAGE("image"),
    BINARY("binary"),
    CURRENCY("currency"),
    DOUBLE("double"),
    BOOLEAN("boolean"),
    INTEGER("integer");

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

    public static Class getTypeClass(PropertyType propertyType) {
        switch (propertyType) {
            case DATE:
                return Date.class;
            case STRING:
                return String.class;
            case GEO_LOCATION:
                return GeoPoint.class;
            case IMAGE:
                return byte[].class;
            case BINARY:
                return byte[].class;
            case CURRENCY:
                return Double.class;
            case BOOLEAN:
                return Boolean.class;
            case DOUBLE:
                return Double.class;
            case INTEGER:
                return Integer.class;
            default:
                throw new LumifyException("Unhandled property type: " + propertyType);
        }
    }
}
