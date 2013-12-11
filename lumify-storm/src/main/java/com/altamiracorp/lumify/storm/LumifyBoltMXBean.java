package com.altamiracorp.lumify.storm;

import javax.management.MXBean;

@MXBean
public interface LumifyBoltMXBean {
    public long getProcessingCount();

    public long getTotalProcessedCount();

    public long getTotalErrorCount();

    public long getAverageProcessingTime();
}
