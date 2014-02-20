package com.altamiracorp.lumify.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CollectionUtil {
    public static <T> List<T> toList(final Iterable<T> it) {
        if (it instanceof List) {
            return (List<T>) it;
        }
        List<T> list = new ArrayList<T>();
        for (T t : it) {
            list.add(t);
        }
        return list;
    }

    public static <T> Iterable<T> toIterable(final T[] arr) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < arr.length;
                    }

                    @Override
                    public T next() {
                        return arr[index++];
                    }

                    @Override
                    public void remove() {
                        throw new RuntimeException("Not supported");
                    }
                };
            }
        };
    }

    public static <T> T single(final Iterable<T> it) {
        Iterator<T> i = it.iterator();
        if (!i.hasNext()) {
            throw new IllegalStateException("No items found.");
        }

        T result = i.next();

        if (i.hasNext()) {
            throw new IllegalStateException("More than 1 item found.");
        }

        return result;
    }

    public static <T> T trySingle(final Iterable<T> it) {
        return trySingle(it, null);
    }

    public static <T> T trySingle(final Iterable<T> it, final T defaultValue) {
        try {
            return single(it);
        } catch (IllegalStateException ise) {
            return defaultValue;
        }
    }

    private CollectionUtil() {
        throw new UnsupportedOperationException("Don't construct a utility class.");
    }
}
