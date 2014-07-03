package io.lumify.model.rabbitmq;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;
import org.securegraph.Graph;

import java.io.IOException;
import java.util.HashSet;

public class RabbitMQWorkQueueRepository extends WorkQueueRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RabbitMQWorkQueueRepository.class);
    private static final String BROADCAST_EXCHANGE_NAME = "exBroadcast";
    private final Channel channel;
    private final Connection connection;
    private HashSet<String> declaredQueues = new HashSet<String>();

    @Inject
    public RabbitMQWorkQueueRepository(Graph graph, Configuration configuration) throws IOException {
        super(graph);
        this.connection = RabbitMQUtils.openConnection(configuration);
        this.channel = RabbitMQUtils.openChannel(this.connection);
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        try {
            ensureBroadcastExchange();
            channel.basicPublish(BROADCAST_EXCHANGE_NAME, "", null, json.toString().getBytes());
        } catch (IOException ex) {
            throw new LumifyException("Could not broadcast json", ex);
        }
    }

    private void ensureBroadcastExchange() throws IOException {
        if (!declaredQueues.contains(BROADCAST_EXCHANGE_NAME)) {
            channel.exchangeDeclare(BROADCAST_EXCHANGE_NAME, "fanout");
            declaredQueues.add(BROADCAST_EXCHANGE_NAME);
        }
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json) {
        try {
            ensureQueue(queueName);
            channel.basicPublish("", queueName, null, json.toString().getBytes());
        } catch (Exception ex) {
            throw new LumifyException("Could not push on queue", ex);
        }
    }

    private void ensureQueue(String queueName) throws IOException {
        if (!declaredQueues.contains(queueName)) {
            channel.queueDeclare(queueName, true, false, false, null);
            declaredQueues.add(queueName);
        }
    }

    @Override
    public Object createSpout(Configuration configuration, String queueName) {
        return new RabbitMQWorkQueueSpout(queueName);
    }

    @Override
    public void flush() {
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            LOGGER.debug("Closing RabbitMQ channel");
            this.channel.close();
            LOGGER.debug("Closing RabbitMQ connection");
            this.connection.close();
        } catch (IOException e) {
            LOGGER.error("Could not close RabbitMQ channel", e);
        }
    }

    @Override
    public void format() {
        try {
            LOGGER.info("deleting queue: %s", GRAPH_PROPERTY_QUEUE_NAME);
            channel.queueDelete(GRAPH_PROPERTY_QUEUE_NAME);
        } catch (IOException e) {
            throw new LumifyException("Could not delete queues", e);
        }
    }

    @Override
    public void subscribeToBroadcastMessages(final BroadcastConsumer broadcastConsumer) {
        try {
            ensureBroadcastExchange();

            String queueName = this.channel.queueDeclare().getQueue();
            this.channel.queueBind(queueName, BROADCAST_EXCHANGE_NAME, "");

            final QueueingConsumer callback = new QueueingConsumer(this.channel);
            this.channel.basicConsume(queueName, true, callback);

            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            QueueingConsumer.Delivery delivery = callback.nextDelivery();
                            try {
                                JSONObject json = new JSONObject(new String(delivery.getBody()));
                                broadcastConsumer.broadcastReceived(json);
                            } catch (Exception ex) {
                                LOGGER.error("problem in broadcast thread", ex);
                            }
                        }
                    } catch (InterruptedException e) {
                        throw new LumifyException("broadcast listener has died", e);
                    }
                }
            });
            t.setName("rabbitmq-subscribe-" + broadcastConsumer.getClass().getName());
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            throw new LumifyException("Could not subscribe to broadcasts", e);
        }
    }
}
