package io.lumify.core.util;

import java.util.*;
import java.util.concurrent.*;

public class WorkerPool {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkerPool.class);
    private ExecutorService executorService;

    /**
     * Create a pool with the specified number of threads.
     *
     * @param nThreads
     */
    public WorkerPool(int nThreads) {
        LOGGER.debug("initializing worker pool with %d threads", nThreads);

        executorService = LOGGER.isDebugEnabled() ? new MetricReportingExecutorService(LOGGER, nThreads) : Executors.newFixedThreadPool(nThreads);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdownAndAwaitTermination(10);
            }
        });
    }

    private void shutdownAndAwaitTermination(int seconds) {
        // disable submission of new tasks
        executorService.shutdown();
        try {
            // wait for existing tasks to terminate
            if (!executorService.awaitTermination(seconds, TimeUnit.SECONDS)) {
                // cancel lingering tasks
                executorService.shutdownNow();
                // wait for lingering tasks to terminate
                if (!executorService.awaitTermination(seconds, TimeUnit.SECONDS)) {
                    System.err.println("executorService did not terminate!");
                }
            }
        } catch (InterruptedException ie) {
            // (re-)cancel if current thread also interrupted
            executorService.shutdownNow();
            // preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Execute the {@link Callable} tasks in parallel (per the configured size of the {@link WorkerPool}) and wait for them to complete.
     *
     * @param tasks a map of {@link Callable}s with keys by which you will be able to access each return value
     * @return the return values of each {@link Callable}s mapped by their input key
     */
    public <K, V> Map<K, V> invokeAll(Map<K, Callable<V>> tasks) {
        String caller = LOGGER.isDebugEnabled() ? Thread.currentThread().getStackTrace()[2].toString() : "n/a";
        LOGGER.debug("[%s] is invoking %d mapped tasks", caller, tasks.size());

        List<K> orderedKeys = new ArrayList<K>(tasks.size());
        List<Callable<V>> orderedTasks = new ArrayList<Callable<V>>(tasks.size());
        for (Map.Entry<K, Callable<V>> entry : tasks.entrySet()) {
            orderedKeys.add(entry.getKey());
            orderedTasks.add(entry.getValue());
        }

        try {
            long start = System.currentTimeMillis();
            List<Future<V>> executorResults = executorService.invokeAll(orderedTasks);
            long finish = System.currentTimeMillis();
            LOGGER.debug("[%s] invoked %d mapped tasks in %d ms", caller, tasks.size(), finish - start);

            Map<K, V> mappedResults = new LinkedHashMap<K, V>(tasks.size());
            for (int i = 0; i < tasks.size(); i++) {
                K key = orderedKeys.get(i);
                V result = executorResults.get(i).get();
                mappedResults.put(key, result);
            }
            return mappedResults;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute the {@link Callable} tasks in parallel (per the configured size of the {@link WorkerPool}) and wait for them to complete.
     *
     * @param tasks a list of {@link Callable}s
     * @return the ordered return values
     */
    public <T> List<T> invokeAll(List<Callable<T>> tasks) {
        String caller = LOGGER.isDebugEnabled() ? Thread.currentThread().getStackTrace()[2].toString() : "n/a";
        LOGGER.debug("[%s] is invoking %d listed tasks", caller, tasks.size());

        try {
            long start = System.currentTimeMillis();
            List<Future<T>> executorResults = executorService.invokeAll(tasks);
            long finish = System.currentTimeMillis();
            LOGGER.debug("[%s] invoked %d listed tasks in %d ms", caller, tasks.size(), finish - start);

            List<T> results = new ArrayList<T>(tasks.size());
            for (Future<T> future : executorResults) {
                results.add(future.get());
            }
            return results;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        WorkerPool workerPool = new WorkerPool(3);
        mapExample(workerPool);
        listExample(workerPool);
        slowExample(workerPool, 5);
        if (LOGGER.isDebugEnabled()) {
            ((MetricReportingExecutorService) workerPool.executorService).tick();
            ((MetricReportingExecutorService) workerPool.executorService).report();
        }
        System.exit(0);
    }

    /**
     * Calculate the squares of the integers 1 through 10.
     *
     * @param workerPool
     */
    private static void mapExample(WorkerPool workerPool) {
        Map<Integer, Callable<Integer>> taskMap = new HashMap<Integer, Callable<Integer>>();
        for (int i = 1; i <= 10; i++) {
            final int input = i;
            Callable<Integer> callable = new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    // do all your parallelizable work here
                    return input * input;
                }
            };
            taskMap.put(i, callable);
        }

        Map<Integer, Integer> resultMap = workerPool.invokeAll(taskMap);

        for (Map.Entry<Integer, Integer> entry : resultMap.entrySet()) {
            System.out.println("key: " + entry.getKey() + ", value: " + entry.getValue());
        }
    }

    /**
     * Return strings including the integers -1 through -10.
     *
     * @param workerPool
     */
    private static void listExample(WorkerPool workerPool) {
        List<Callable<String>> taskList = new ArrayList<Callable<String>>();
        for (int i = 1; i <= 10; i++) {
            final String input = Integer.toString(-1 * i);
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    // do all your parallelizable work here
                    return "R(" + input + ")";
                }
            };
            taskList.add(callable);
        }

        List<String> resultList = workerPool.invokeAll(taskList);

        for (String result : resultList) {
            System.out.println("result: " + result);
        }
    }

    private static void slowExample(WorkerPool workerPool, int nTasks) {
        List<Callable<Integer>> taskList = new ArrayList<Callable<Integer>>();
        Random random = new Random();
        int maxWait = 10000;
        for (int i = 1; i <= nTasks; i++) {
            final int wait = random.nextInt(maxWait);
            Callable<Integer> callable = new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    Thread.sleep(wait);
                    return wait;
                }
            };
            taskList.add(callable);
        }

        long start = System.currentTimeMillis();
        List<Integer> waits = workerPool.invokeAll(taskList);
        long finish = System.currentTimeMillis();

        int longestWait = 0;
        int totalWait = 0;
        for (int wait : waits) {
            longestWait = wait > longestWait ? wait : longestWait;
            totalWait += wait;
        }
        System.out.println(String.format("elapsed time: %5d ms (%.2f%%)", finish - start, 100 * (1.0 * (finish - start)) / totalWait));
        System.out.println(String.format("longest wait: %5d ms", longestWait));
        System.out.println(String.format("average wait: %5d ms", totalWait / nTasks));
        System.out.println(String.format("total wait:   %5d ms", totalWait));
    }
}
