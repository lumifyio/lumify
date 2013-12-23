package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.metrics.MetricsManager;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public abstract class ThreadedTeeInputStreamWorker<TResult, TData> implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedTeeInputStreamWorker.class.getName());
    private final Counter totalProcessedCounter;
    private final Counter processingCounter;
    private final Counter totalErrorCounter;
    private final Timer processingTimeTimer;
    private boolean stopped;
    private final Queue<Work> workItems = new LinkedList<Work>();
    private final Queue<WorkResult<TResult>> workResults = new LinkedList<WorkResult<TResult>>();

    public ThreadedTeeInputStreamWorker() {
        MetricsManager metricsManager = MetricsManager.getInstance();
        String namePrefix = metricsManager.getNamePrefix(this);
        totalProcessedCounter = metricsManager.getRegistry().counter(namePrefix + "total-processed");
        processingCounter = metricsManager.getRegistry().counter(namePrefix + "processing");
        totalErrorCounter = metricsManager.getRegistry().counter(namePrefix + "total-errors");
        processingTimeTimer = metricsManager.getRegistry().timer(namePrefix + "processing-time");
    }

    @Override
    public final void run() {
        stopped = false;
        try {
            while (!stopped) {
                Work work;
                synchronized (workItems) {
                    if (workItems.size() == 0) {
                        workItems.wait(1000);
                        continue;
                    }
                    work = workItems.remove();
                }
                InputStream in = work.getIn();
                try {
                    LOGGER.debug("BEGIN doWork (" + getClass().getName() + ")");
                    TResult result;
                    Timer.Context timerContext = processingTimeTimer.time();
                    processingCounter.inc();
                    try {
                        result = doWork(in, work.getData());
                    } finally {
                        LOGGER.debug("END doWork (" + getClass().getName() + ")");
                        processingCounter.dec();
                        totalProcessedCounter.inc();
                        timerContext.stop();
                    }
                    synchronized (workResults) {
                        workResults.add(new WorkResult<TResult>(result, null));
                        workResults.notifyAll();
                    }
                } catch (Exception ex) {
                    totalErrorCounter.inc();
                    synchronized (workResults) {
                        workResults.add(new WorkResult<TResult>(null, ex));
                        workResults.notifyAll();
                    }
                } finally {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        synchronized (workResults) {
                            workResults.add(new WorkResult<TResult>(null, ex));
                            workResults.notifyAll();
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            LOGGER.error("thread was interrupted", ex);
        }
    }

    protected abstract TResult doWork(InputStream work, TData data) throws Exception;

    public void enqueueWork(InputStream in, TData data) {
        synchronized (workItems) {
            workItems.add(new Work(in, data));
            workItems.notifyAll();
        }
    }

    public WorkResult<TResult> dequeueResult() {
        synchronized (workResults) {
            if (workResults.size() == 0) {
                long startTime = new Date().getTime();
                while (workResults.size() == 0 && (new Date().getTime() - startTime < 10 * 1000)) {
                    try {
                        LOGGER.warn("worker has zero results. sleeping waiting for results.");
                        workResults.wait(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return workResults.remove();
        }
    }

    public void stop() {
        stopped = true;
    }

    private class Work {
        private final InputStream in;
        private final TData data;

        public Work(InputStream in, TData data) {
            this.in = in;
            this.data = data;
        }

        private InputStream getIn() {
            return in;
        }

        private TData getData() {
            return data;
        }
    }

    public static class WorkResult<TResult> {
        private final TResult result;
        private final Exception error;

        public WorkResult(TResult result, Exception error) {
            this.result = result;
            this.error = error;
        }

        public Exception getError() {
            return error;
        }

        public TResult getResult() {
            return result;
        }
    }
}
