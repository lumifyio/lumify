package com.altamiracorp.lumify.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ThreadedTeeInputStreamWorker<TResult, TData> implements Runnable, ThreadedTeeInputStreamWorkerMXBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedTeeInputStreamWorker.class.getName());
    private boolean stopped;
    private final Queue<Work> workItems = new LinkedList<Work>();
    private final Queue<WorkResult<TResult>> workResults = new LinkedList<WorkResult<TResult>>();
    private AtomicLong totalProcessedCount = new AtomicLong();
    private AtomicLong processingCount = new AtomicLong();
    private AtomicLong totalErrorCount = new AtomicLong();
    private long averageProcessingTime;

    public ThreadedTeeInputStreamWorker() {
        try {
            registerJmxBean();
        } catch (Exception ex) {
            LOGGER.error("Could not register JMX Bean", ex);
        }
    }

    protected void registerJmxBean() throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, MalformedObjectNameException {
        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
        for (int suffix = 0; ; suffix++) {
            ObjectName beanName = new ObjectName(getBaseJmxObjectName() + ":type=" + getClass().getName() + "-" + suffix);
            if (beanServer.isRegistered(beanName)) {
                continue;
            }
            beanServer.registerMBean(this, beanName);
            break;
        }
    }

    protected String getBaseJmxObjectName() {
        return "com.altamiracorp.lumify.worker";
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
                long startTime = System.currentTimeMillis();
                processingCount.getAndIncrement();
                InputStream in = work.getIn();
                try {
                    LOGGER.debug("BEGIN doWork (" + getClass().getName() + ")");
                    TResult result;
                    try {
                        result = doWork(in, work.getData());
                    } finally {
                        LOGGER.debug("END doWork (" + getClass().getName() + ")");
                        processingCount.getAndDecrement();
                        totalProcessedCount.getAndIncrement();
                        long endTime = System.currentTimeMillis();
                        long processingTime = endTime - startTime;
                        this.averageProcessingTime = (((totalProcessedCount.get() - 1) * this.averageProcessingTime) + processingTime) / totalProcessedCount.get();
                    }
                    synchronized (workResults) {
                        workResults.add(new WorkResult<TResult>(result, null));
                        workResults.notifyAll();
                    }
                } catch (Exception ex) {
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

    @Override
    public long getProcessingCount() {
        return this.processingCount.get();
    }

    @Override
    public long getTotalProcessedCount() {
        return this.totalProcessedCount.get();
    }

    @Override
    public long getAverageProcessingTime() {
        return this.averageProcessingTime;
    }

    @Override
    public long getTotalErrorCount() {
        return this.totalErrorCount.get();
    }
}
