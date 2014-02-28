package com.altamiracorp.lumify.core.util;

import java.util.*;

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

    public static <T> T singleOrDefault(final Iterable<T> it, T defaultValue) {
        Iterator<T> i = it.iterator();
        if (!i.hasNext()) {
            return defaultValue;
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

    public static <TKey, TValue> Map<TKey, TValue> toMap(final Iterable<TValue> values, ValueToKey<TValue, TKey> valueToKey) {
        Map<TKey, TValue> result = new HashMap<TKey, TValue>();
        for (TValue v : values) {
            result.put(valueToKey.toKey(v), v);
        }
        return result;
    }

    public static interface ValueToKey<TValue, TKey> {
        TKey toKey(TValue v);
    }

    private CollectionUtil() {
        throw new UnsupportedOperationException("Don't construct a utility class.");
    }
}
