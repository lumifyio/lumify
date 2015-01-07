package io.lumify.model.rabbitmq;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.WorkerSpout;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;
import org.securegraph.Graph;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RabbitMQWorkQueueRepository extends WorkQueueRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RabbitMQWorkQueueRepository.class);
    private static final String BROADCAST_EXCHANGE_NAME = "exBroadcast";
    private final Connection connection;
    private final Channel channel;
    private QueueingConsumer longRunningProcessCallback;
    private Set<String> declaredQueues = new HashSet<String>();

    @Inject
    public RabbitMQWorkQueueRepository(Graph graph, Configuration configuration) throws IOException {
        super(graph);
        this.connection = RabbitMQUtils.openConnection(configuration);
        this.channel = RabbitMQUtils.openChannel(this.connection);
        this.channel.exchangeDeclare(BROADCAST_EXCHANGE_NAME, "fanout");
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        try {
            LOGGER.debug("publishing message to broadcast exchange [%s]: %s", BROADCAST_EXCHANGE_NAME, json.toString());
            channel.basicPublish(BROADCAST_EXCHANGE_NAME, "", null, json.toString().getBytes());
        } catch (IOException ex) {
            throw new LumifyException("Could not broadcast json", ex);
        }
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json) {
        try {
            ensureQueue(queueName);
            LOGGER.debug("enqueueing message to queue [%s]: %s", queueName, json.toString());
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
            channel.queueDelete(LONG_RUNNING_PROCESS_QUEUE_NAME);
        } catch (IOException e) {
            throw new LumifyException("Could not delete queues", e);
        }
    }

    @Override
    public void subscribeToBroadcastMessages(final BroadcastConsumer broadcastConsumer) {
        try {
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
                                LOGGER.debug("received message from broadcast exchange [%s]: %s", BROADCAST_EXCHANGE_NAME, json.toString());
                                broadcastConsumer.broadcastReceived(json);
                            } catch (Throwable ex) {
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

    @Override
    public LongRunningProcessMessage getNextLongRunningProcessMessage() {
        try {
            synchronized (this) {
                if (longRunningProcessCallback == null) {
                    channel.queueDeclare(LONG_RUNNING_PROCESS_QUEUE_NAME, true, false, false, null);
                    longRunningProcessCallback = new QueueingConsumer(channel);
                    channel.basicConsume(LONG_RUNNING_PROCESS_QUEUE_NAME, false, longRunningProcessCallback);
                }
            }
            QueueingConsumer.Delivery delivery = longRunningProcessCallback.nextDelivery(1000);
            if (delivery == null) {
                return null;
            }
            JSONObject queueItem = new JSONObject(new String(delivery.getBody()));
            long deliveryTag = delivery.getEnvelope().getDeliveryTag();
            LOGGER.debug("received message from long running process queue [%s]: %s", LONG_RUNNING_PROCESS_QUEUE_NAME, queueItem.toString());
            return new RabbitMQLongRunningProcessMessage(queueItem, deliveryTag);
        } catch (Exception e) {
            throw new LumifyException("Could not read long running process queue", e);
        }
    }

    @Override
    public WorkerSpout createWorkerSpout() {
        return InjectHelper.inject(new RabbitMQWorkQueueSpout(GRAPH_PROPERTY_QUEUE_NAME));
    }

    private class RabbitMQLongRunningProcessMessage extends LongRunningProcessMessage {
        private final long deliveryTag;
        private final long startTime;

        public RabbitMQLongRunningProcessMessage(JSONObject message, long deliveryTag) {
            super(message);
            this.startTime = System.currentTimeMillis();
            this.deliveryTag = deliveryTag;
        }

        @Override
        public void complete(Throwable ex) {
            try {
                if (ex != null) {
                    throw ex;
                }
                long endTime = System.currentTimeMillis();
                LOGGER.debug("ack'ing message from long running process queue [%s]: %s (work time: %dms)", LONG_RUNNING_PROCESS_QUEUE_NAME, getMessage().toString(), endTime - startTime);
                channel.basicAck(deliveryTag, false);
            } catch (Throwable ackException) {
                LOGGER.error("problem in long running process thread", ex);
                try {
                    channel.basicNack(deliveryTag, false, false);
                } catch (IOException nackException) {
                    LOGGER.error("Could not nack message: " + deliveryTag, nackException);
                }
            }
        }
    }
}
