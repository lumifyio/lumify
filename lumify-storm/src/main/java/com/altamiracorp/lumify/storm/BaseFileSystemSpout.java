package com.altamiracorp.lumify.storm;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.altamiracorp.lumify.core.InjectHelper;
import com.altamiracorp.lumify.core.metrics.MetricsManager;
import com.altamiracorp.lumify.core.storm.StormBootstrap;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Module;

import java.util.Map;

public abstract class BaseFileSystemSpout extends BaseRichSpout {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseFileSystemSpout.class);
    public static final String DATADIR_CONFIG_NAME = "datadir";
    private SpoutOutputCollector collector;
    private Map<String, String> workingFiles;
    private Counter totalProcessedCounter;
    private Counter totalErrorCounter;
    private MetricsManager metricsManager;

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields(FieldNames.FILE_NAME));
    }

    @Override
    public void open(final Map conf, TopologyContext context, SpoutOutputCollector collector) {
        LOGGER.info("Configuring environment for spout: %s-%d", context.getThisComponentId(), context.getThisTaskId());
        this.collector = collector;
        InjectHelper.inject(this, new InjectHelper.ModuleMaker() {
            @Override
            public Module createModule() {
                return StormBootstrap.create(conf);
            }
        });
        workingFiles = Maps.newHashMap();

        String namePrefix = metricsManager.getNamePrefix(this, getPath());
        registerMetrics(metricsManager, namePrefix);
    }

    protected void registerMetrics(MetricsManager metricsManager, String namePrefix) {
        totalProcessedCounter = metricsManager.getRegistry().counter(namePrefix + "total-processed");
        totalErrorCounter = metricsManager.getRegistry().counter(namePrefix + "total-errors");
        metricsManager.getRegistry().register(namePrefix + "in-process",
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return workingFiles.size();
                    }
                });
    }

    protected abstract String getPath();

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
        LOGGER.debug("Emitting value: %s", path);
        collector.emit(new Values(path), path);
    }

    @Override
    public final void ack(Object msgId) {
        LOGGER.debug("received ack on: %s", msgId);
        try {
            safeAck(msgId);
            totalProcessedCounter.inc();
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
        LOGGER.error("received fail on: %s", msgId);
        totalErrorCounter.inc();
        if (workingFiles.containsKey(msgId)) {
            workingFiles.remove(msgId);
        }
        super.fail(msgId);
    }

    @Inject
    public void setMetricsManager(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }
}
