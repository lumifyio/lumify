package com.altamiracorp.lumify.storm;

import com.altamiracorp.lumify.core.metrics.JmxMetricsManager;
import com.altamiracorp.lumify.core.metrics.PausableTimerContext;
import com.altamiracorp.lumify.core.metrics.PausableTimerContextAware;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class GraphPropertyThreadedWrapper implements Runnable {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphPropertyThreadedWrapper.class);
    private final GraphPropertyWorker worker;

    public GraphPropertyThreadedWrapper(GraphPropertyWorker worker) {
        this.worker = worker;
    }

    private Counter totalProcessedCounter = null;
    private Counter processingCounter;
    private Counter totalErrorCounter;
    private Timer processingTimeTimer;
    private boolean stopped;
    private final Queue<Work> workItems = new LinkedList<Work>();
    private final Queue<WorkResult<GraphPropertyWorkResult>> workResults = new LinkedList<WorkResult<GraphPropertyWorkResult>>();
    private JmxMetricsManager metricsManager;

    @Override
    public final void run() {
        ensureJmxInitialized();

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
                    LOGGER.debug("BEGIN doWork (%s)", getClass().getName());
                    GraphPropertyWorkResult result;
                    PausableTimerContext timerContext = new PausableTimerContext(processingTimeTimer);
                    if (in instanceof PausableTimerContextAware) {
                        ((PausableTimerContextAware) in).setPausableTimerContext(timerContext);
                    }
                    processingCounter.inc();
                    try {
                        result = this.worker.execute(in, work.getData());
                    } finally {
                        LOGGER.debug("END doWork (%s)", getClass().getName());
                        processingCounter.dec();
                        totalProcessedCounter.inc();
                        timerContext.stop();
                    }
                    synchronized (workResults) {
                        workResults.add(new WorkResult<GraphPropertyWorkResult>(result, null));
                        workResults.notifyAll();
                    }
                } catch (Exception ex) {
                    totalErrorCounter.inc();
                    synchronized (workResults) {
                        workResults.add(new WorkResult<GraphPropertyWorkResult>(null, ex));
                        workResults.notifyAll();
                    }
                } finally {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        synchronized (workResults) {
                            workResults.add(new WorkResult<GraphPropertyWorkResult>(null, ex));
                            workResults.notifyAll();
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            LOGGER.error("thread was interrupted", ex);
        }
    }

    private void ensureJmxInitialized() {
        if (totalProcessedCounter == null) {
            String namePrefix = metricsManager.getNamePrefix(this);
            totalProcessedCounter = metricsManager.counter(namePrefix + "total-processed");
            processingCounter = metricsManager.counter(namePrefix + "processing");
            totalErrorCounter = metricsManager.counter(namePrefix + "total-errors");
            processingTimeTimer = metricsManager.timer(namePrefix + "processing-time");
        }
    }

    public void enqueueWork(InputStream in, GraphPropertyWorkData data) {
        synchronized (workItems) {
            workItems.add(new Work(in, data));
            workItems.notifyAll();
        }
    }

    public WorkResult<GraphPropertyWorkResult> dequeueResult() {
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

    public GraphPropertyWorker getWorker() {
        return worker;
    }

    private class Work {
        private final InputStream in;
        private final GraphPropertyWorkData data;

        public Work(InputStream in, GraphPropertyWorkData data) {
            this.in = in;
            this.data = data;
        }

        private InputStream getIn() {
            return in;
        }

        private GraphPropertyWorkData getData() {
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

    @Inject
    public void setMetricsManager(JmxMetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }
}
