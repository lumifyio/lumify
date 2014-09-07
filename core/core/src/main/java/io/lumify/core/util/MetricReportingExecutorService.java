package io.lumify.core.util;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricReportingExecutorService extends ThreadPoolExecutor {
    private LumifyLogger logger;
    private ScheduledExecutorService scheduledExecutorService;
    private FixedSizeCircularLinkedList<AtomicInteger> activity;

    public MetricReportingExecutorService(LumifyLogger logger, int nThreads) {
        super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        this.logger = logger;

        activity = new FixedSizeCircularLinkedList<AtomicInteger>(16, AtomicInteger.class);

        // TODO: max executing concurrently
        // TODO: max waiting (via ref to queue?)

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
        activity.head().incrementAndGet();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
    }

    public void tick() {
        activity.rotateForward();
        activity.head().set(0);
    }

    public void report() {
        List<AtomicInteger> list = activity.readBackward(15);
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
        logger.debug("%d / %.2f / %.2f / %s", one, five / 5.0, fifteen / 15.0, activity.toString());
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
