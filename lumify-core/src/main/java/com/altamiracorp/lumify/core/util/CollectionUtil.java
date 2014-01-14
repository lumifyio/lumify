package com.altamiracorp.lumify.core.util;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtil {
    public static <T> List<T> toList(Iterable<T> it) {
        List<T> list = new ArrayList<T>();
        for (T t : it) {
            list.add(t);
        }
        return list;
    }
}
