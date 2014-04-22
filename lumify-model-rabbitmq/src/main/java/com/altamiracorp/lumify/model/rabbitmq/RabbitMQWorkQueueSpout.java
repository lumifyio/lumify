package com.altamiracorp.lumify.model.rabbitmq;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import com.altamiracorp.lumify.core.config.Configuration;

import java.util.Map;

public class RabbitMQWorkQueueSpout extends BaseRichSpout {
    public RabbitMQWorkQueueSpout(Configuration configuration, String queueName) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void nextTuple() {
        throw new RuntimeException("Not implemented");
    }
}
