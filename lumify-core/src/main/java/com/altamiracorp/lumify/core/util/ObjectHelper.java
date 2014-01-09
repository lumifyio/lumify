package com.altamiracorp.lumify.core.util;

public class ObjectHelper {
    public static String toStringOrNull(Object o) {
        if (o == null) {
            return null;
        }
        return o.toString();
    }
}
