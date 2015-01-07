package io.lumify.core.util;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricReportingExecutorService extends ThreadPoolExecutor {
    private LumifyLogger logger;
    private ScheduledExecutorService scheduledExecutorService;
    private FixedSizeCircularLinkedList<AtomicInteger> executionCount;
    private FixedSizeCircularLinkedList<AtomicInteger> maxActive;
    private FixedSizeCircularLinkedList<AtomicInteger> maxWaiting;

    public MetricReportingExecutorService(LumifyLogger logger, int nThreads) {
        super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        this.logger = logger;

        executionCount = new FixedSizeCircularLinkedList<AtomicInteger>(16, AtomicInteger.class);
        maxActive = new FixedSizeCircularLinkedList<AtomicInteger>(16, AtomicInteger.class);
        maxWaiting = new FixedSizeCircularLinkedList<AtomicInteger>(16, AtomicInteger.class);

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 1, 1, TimeUnit.MINUTES);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                report();
            }
        }, 1, 5, TimeUnit.MINUTES);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);

        executionCount.head().incrementAndGet();

        int active = getActiveCount();
        int currentMaxActive = maxActive.head().get();
        if (active > currentMaxActive) {
            maxActive.head().set(active);
        }

        int waiting = getQueue().size();
        int currentMaxWaiting = maxWaiting.head().get();
        if (waiting > currentMaxWaiting) {
            maxWaiting.head().set(waiting);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
    }

    public void tick() {
        executionCount.rotateForward();
        executionCount.head().set(0);

        maxActive.rotateForward();
        maxActive.head().set(0);

        maxWaiting.rotateForward();
        maxWaiting.head().set(0);
    }

    public void report() {
        List<AtomicInteger> executionCountList = executionCount.readBackward(15);
        List<AtomicInteger> maxActiveList = maxActive.readBackward(15);
        List<AtomicInteger> maxWaitingList = maxWaiting.readBackward(15);
        report("executions: ", executionCountList);
        report("max active: ", maxActiveList);
        report("max waiting:", maxWaitingList);
    }

    private void report(String label, List<AtomicInteger> list) {
        int one = list.get(0).get();
        int five = 0;
        int fifteen = 0;
        for (int i = 0; i < 15; i++) {
            int value = list.get(i).get();
            if (i < 5) {
                five += value;
            }
            fifteen += value;
        }
        logger.debug("%s %3d / %6.2f / %6.2f", label, one, five / 5.0, fifteen / 15.0);
    }

    @Override
    public void shutdown() {
        scheduledExecutorService.shutdown();
        super.shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        scheduledExecutorService.awaitTermination(timeout, unit);
        return super.awaitTermination(timeout, unit);
    }

    @Override
    public List<Runnable> shutdownNow() {
        scheduledExecutorService.shutdownNow();
        return super.shutdownNow();
    }
}
