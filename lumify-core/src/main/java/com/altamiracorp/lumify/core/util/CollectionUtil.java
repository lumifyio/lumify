package com.altamiracorp.lumify.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CollectionUtil {
    public static <T> List<T> toList(Iterable<T> it) {
        List<T> list = new ArrayList<T>();
        for (T t : it) {
            list.add(t);
        }
        return list;
    }

    public static <T> T single(Iterable<T> it) {
        Iterator<T> i = it.iterator();
        if (!i.hasNext()) {
            throw new RuntimeException("Iterable has no items");
        }

        T result = i.next();

        if (i.hasNext()) {
            throw new RuntimeException("Iterable has more than one item");
        }

        return result;
    }
}
