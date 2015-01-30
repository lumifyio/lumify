package io.lumify.model.rabbitmq;

import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.WorkerSpout;
import io.lumify.core.ingest.WorkerTuple;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerTuple;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

import java.io.IOException;

public class RabbitMQWorkQueueSpout extends WorkerSpout {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RabbitMQWorkQueueSpout.class);
    private final String queueName;
    private Channel channel;
    private QueueingConsumer consumer;
    private Connection connection;
    private Configuration configuration;

    public RabbitMQWorkQueueSpout(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public void open() {
        try {
            this.connection = RabbitMQUtils.openConnection(configuration);
            this.channel = RabbitMQUtils.openChannel(this.connection);
            this.channel.queueDeclare(queueName, true, false, false, null);
            this.consumer = new QueueingConsumer(channel);
            this.channel.basicConsume(this.queueName, false, consumer);
            this.channel.basicQos(configuration.getInt(Configuration.RABBITMQ_PREFETCH_COUNT, 0));
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
    public WorkerTuple nextTuple() throws InterruptedException {
        QueueingConsumer.Delivery delivery = this.consumer.nextDelivery(100);
        if (delivery == null) {
            return null;
        }
        JSONObject json = new JSONObject(new String(delivery.getBody()));
        LOGGER.debug("emit (%s): %s", this.queueName, json.toString());
        return new GraphPropertyWorkerTuple(delivery.getEnvelope().getDeliveryTag(), json);
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

    protected QueueingConsumer getConsumer () { return this.consumer; }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
