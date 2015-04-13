package io.lumify.core.util;

import io.lumify.core.exception.LumifyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutorServiceUtil {
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static <T> T[] runAllAndWait(Callable<T>... tasks) {
        try {
            List<T> results = new ArrayList<T>(tasks.length);
            List<Future<T>> futures = pool.invokeAll(Arrays.asList(tasks));
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return (T[]) results.toArray();
        } catch (Exception ex) {
            throw new LumifyException("Failed to execute tasks", ex);
        }
    }
}
