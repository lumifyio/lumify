package com.altamiracorp.lumify.storm;

public interface LumifySpoutMXBean {
    public long getWorkingCount();

    public long getToBeProcessedCount();

    public long getTotalProcessedCount();

    public long getTotalErrorCount();

    public String getName();
}
