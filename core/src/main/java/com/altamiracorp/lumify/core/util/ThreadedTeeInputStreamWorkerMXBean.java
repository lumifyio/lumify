package com.altamiracorp.lumify.core.util;

public interface ThreadedTeeInputStreamWorkerMXBean {
    public long getProcessingCount();

    public long getTotalProcessedCount();

    public long getTotalErrorCount();

    public long getAverageProcessingTime();
}
