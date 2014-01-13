package com.altamiracorp.lumify.model;

import backtype.storm.spout.Scheme;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.metrics.JmxMetricsManager;
import com.codahale.metrics.Counter;
import com.google.inject.Inject;
import storm.kafka.KafkaConfig;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;

import java.util.Map;

public class LumifyKafkaSpout extends KafkaSpout {
    private final String queueName;
    private Counter totalProcessedCounter;
    private Counter totalErrorCounter;
    private JmxMetricsManager metricsManager;
    private Scheme scheme;

    public static final long KAFKA_START_OFFSET_TIME_LATEST = -1;
    public static final long KAFKA_START_OFFSET_TIME_EARLIEST = -2;

    public LumifyKafkaSpout(Configuration configuration, String queueName, Long startOffsetTime) {
        this(configuration, queueName, new KafkaJsonEncoder(), startOffsetTime);
    }
    
    protected LumifyKafkaSpout(Configuration configuration, String queueName, Scheme scheme, Long startOffsetTime) {
        super(createConfig(configuration, queueName, scheme, startOffsetTime));
        this.queueName = queueName;
        this.scheme = scheme;
    }

    private static SpoutConfig createConfig(Configuration configuration, String queueName, Scheme scheme, Long startOffsetTime) {
        SpoutConfig spoutConfig = new SpoutConfig(
                new KafkaConfig.ZkHosts(configuration.get(Configuration.ZK_SERVERS), "/kafka/brokers"),
                queueName,
                "/kafka/consumers",
                queueName);
        spoutConfig.scheme = scheme;
        if (startOffsetTime != null) {
            spoutConfig.forceStartOffsetTime(startOffsetTime);
        }
        return spoutConfig;
    }

    @Override
    public void open(final Map conf, TopologyContext topologyContext, SpoutOutputCollector collector) {
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(new Configuration(conf)));
        String namePrefix = metricsManager.getNamePrefix(this, queueName);
        totalProcessedCounter = metricsManager.getRegistry().counter(namePrefix + "total-processed");
        totalErrorCounter = metricsManager.getRegistry().counter(namePrefix + "total-errors");

        super.open(conf, topologyContext, collector);
    }

    @Override
    public void ack(Object o) {
        super.ack(o);
        totalProcessedCounter.inc();
    }

    @Override
    public void fail(Object o) {
        super.fail(o);
        totalErrorCounter.inc();
    }

    protected final Scheme getScheme() {
        return scheme;
    }
    
    @Inject
    public void setMetricsManager(JmxMetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }
}
