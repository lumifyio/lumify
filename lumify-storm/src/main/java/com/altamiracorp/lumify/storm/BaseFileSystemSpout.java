package com.altamiracorp.lumify.storm;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public abstract class BaseFileSystemSpout extends BaseRichSpout implements LumifySpoutMXBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseFileSystemSpout.class);
    public static final String DATADIR_CONFIG_NAME = "datadir";
    private SpoutOutputCollector collector;
    private Map<String, String> workingFiles;
    private final AtomicLong totalProcessed = new AtomicLong();
    private final AtomicLong totalErrorCount = new AtomicLong();

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields(FieldNames.FILE_NAME));
    }

    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        LOGGER.info(String.format("Configuring environment for spout: %s-%d", context.getThisComponentId(), context.getThisTaskId()));
        this.collector = collector;
        workingFiles = Maps.newHashMap();

        try {
            JmxBeanHelper.registerJmxBean(this, JmxBeanHelper.SPOUT_PREFIX);
        } catch (Exception ex) {
            LOGGER.error("Could not register JMX bean", ex);
        }
    }

    protected SpoutOutputCollector getCollector() {
        return collector;
    }

    protected boolean isInWorkingSet(String fileName) {
        return workingFiles.containsKey(fileName);
    }

    protected String getPathFromMessageId(Object msgId) {
        return workingFiles.get(msgId);
    }

    protected void emit(String path) {
        workingFiles.put(path, path);
        LOGGER.info("Emitting value: " + path);
        collector.emit(new Values(path), path);
    }

    @Override
    public final void ack(Object msgId) {
        LOGGER.debug("received ack on: " + msgId);
        try {
            safeAck(msgId);
            totalProcessed.incrementAndGet();
            if (workingFiles.containsKey(msgId)) {
                workingFiles.remove(msgId);
            }
            super.ack(msgId);
        } catch (Exception ex) {
            LOGGER.error("exception during ack of: " + msgId, ex);
            collector.reportError(ex);
        }
    }

    protected void safeAck(Object msgId) throws Exception {
    }

    @Override
    public void fail(Object msgId) {
        LOGGER.error("received fail on: " + msgId);
        totalErrorCount.incrementAndGet();
        if (workingFiles.containsKey(msgId)) {
            workingFiles.remove(msgId);
        }
        super.fail(msgId);
    }

    @Override
    public long getWorkingCount() {
        return workingFiles.size();
    }

    @Override
    public long getTotalProcessedCount() {
        return totalProcessed.get();
    }

    @Override
    public long getTotalErrorCount() {
        return totalErrorCount.get();
    }
}
