package com.altamiracorp.lumify.storm;

import backtype.storm.spout.Scheme;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.model.KafkaJsonEncoder;
import storm.kafka.KafkaConfig;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class LumifyKafkaSpout extends KafkaSpout implements LumifySpoutMXBean {
    private final String queueName;
    private AtomicLong totalProcessedCount = new AtomicLong();
    private AtomicLong totalErrorCount = new AtomicLong();

    public LumifyKafkaSpout(Configuration configuration, String queueName) {
        super(createConfig(configuration, queueName, null));
        this.queueName = queueName;
    }

    private static SpoutConfig createConfig(Configuration configuration, String queueName, Scheme scheme) {
        if (scheme == null) {
            scheme = new KafkaJsonEncoder();
        }
        SpoutConfig spoutConfig = new SpoutConfig(
                new KafkaConfig.ZkHosts(configuration.get(Configuration.ZK_SERVERS), "/kafka/brokers"),
                queueName,
                "/kafka/consumers",
                queueName);
        spoutConfig.scheme = scheme;
        return spoutConfig;
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector collector) {
        try {
            JmxBeanHelper.registerJmxBean(this, JmxBeanHelper.SPOUT_PREFIX);
        } catch (Exception ex) {
            collector.reportError(ex);
        }
        super.open(map, topologyContext, collector);
    }

    @Override
    public void ack(Object o) {
        super.ack(o);
        totalProcessedCount.incrementAndGet();
    }

    @Override
    public void fail(Object o) {
        super.fail(o);
        totalErrorCount.incrementAndGet();
    }

    @Override
    public long getWorkingCount() {
        return 0;
    }

    @Override
    public long getTotalProcessedCount() {
        return totalProcessedCount.get();
    }

    @Override
    public long getTotalErrorCount() {
        return totalErrorCount.get();
    }

    @Override
    public long getToBeProcessedCount() {
        return 0;
    }

    @Override
    public String getName() {
        return queueName;
    }
}
