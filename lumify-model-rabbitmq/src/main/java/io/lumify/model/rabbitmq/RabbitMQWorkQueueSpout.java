package io.lumify.model.rabbitmq;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class RabbitMQWorkQueueSpout extends BaseRichSpout {
    private static final long serialVersionUID = -7022068682287675679L;
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RabbitMQWorkQueueSpout.class);
    private final String queueName;
    private Configuration configuration;
    private Channel channel;
    private SpoutOutputCollector collector;
    private QueueingConsumer consumer;
    private Connection connection;

    public RabbitMQWorkQueueSpout(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("json"));
    }

    @Override
    public void open(Map conf, TopologyContext topologyContext, SpoutOutputCollector collector) {
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(new Configuration(conf)));

        try {
            this.connection = RabbitMQUtils.openConnection(configuration);
            this.channel = RabbitMQUtils.openChannel(this.connection);
            this.collector = collector;
            this.channel.queueDeclare(queueName, false, false, false, null);
            this.consumer = new QueueingConsumer(channel);
            this.channel.basicConsume(this.queueName, false, consumer);
        } catch (IOException ex) {
            throw new LumifyException("Could not startup RabbitMQ", ex);
        }
    }

    @Override
    public void close() {
        super.close();
        try {
            LOGGER.debug("Closing RabbitMQ channel");
            this.channel.close();
            LOGGER.debug("Closing RabbitMQ connection");
            this.connection.close();
        } catch (IOException ex) {
            LOGGER.error("Could not close RabbitMQ connection and channel", ex);
        }
    }

    @Override
    public void nextTuple() {
        try {
            QueueingConsumer.Delivery delivery = this.consumer.nextDelivery(100);
            if (delivery == null) {
                Utils.sleep(1000);
                return;
            }
            JSONObject json = new JSONObject(new String(delivery.getBody()));
            LOGGER.debug("emit (%s): %s", this.queueName, json.toString());
            this.collector.emit(new Values(json.toString()), delivery.getEnvelope().getDeliveryTag());
        } catch (InterruptedException ex) {
            LOGGER.error("Could not consume", ex);
            this.collector.reportError(ex);
        }
    }

    @Override
    public void ack(Object msgId) {
        super.ack(msgId);
        long deliveryTag = (Long) msgId;
        try {
            this.channel.basicAck(deliveryTag, false);
        } catch (IOException ex) {
            LOGGER.error("Could not ack: %d", deliveryTag, ex);
        }
    }

    @Override
    public void fail(Object msgId) {
        super.fail(msgId);
        long deliveryTag = (Long) msgId;
        try {
            this.channel.basicNack(deliveryTag, false, false);
        } catch (IOException ex) {
            LOGGER.error("Could not ack: %d", deliveryTag, ex);
        }
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
