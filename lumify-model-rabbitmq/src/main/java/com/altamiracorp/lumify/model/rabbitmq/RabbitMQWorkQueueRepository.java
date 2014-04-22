package com.altamiracorp.lumify.model.rabbitmq;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RabbitMQWorkQueueRepository extends WorkQueueRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RabbitMQWorkQueueRepository.class);
    private static final int DEFAULT_PORT = 5672;
    private final Channel channel;
    private HashSet<String> declaredQueues = new HashSet<String>();

    @Inject
    public RabbitMQWorkQueueRepository(Graph graph, Configuration configuration) throws IOException {
        super(graph);
        ConnectionFactory factory = new ConnectionFactory();
        Address[] addresses = getAddresses(configuration);
        if (addresses.length == 0) {
            throw new LumifyException("Could not configure RabbitMQ. No addresses specified. expecting configuration parameter 'rabbitmq.addr.0.host'.");
        }
        Connection connection = factory.newConnection(addresses);
        this.channel = connection.createChannel();
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json) {
        try {
            if (!declaredQueues.contains(queueName)) {
                channel.queueDeclare(queueName, false, false, false, null);
                declaredQueues.add(queueName);
            }
            channel.basicPublish("", queueName, null, json.toString().getBytes());
        } catch (Exception ex) {
            throw new LumifyException("Could not push on queue", ex);
        }
    }

    @Override
    public Object createSpout(Configuration configuration, String queueName) {
        return new RabbitMQWorkQueueSpout(configuration, queueName);
    }

    @Override
    public void flush() {
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

    private static Address[] getAddresses(Configuration configuration) {
        List<Address> addresses = new ArrayList<Address>();
        for (int i = 0; i < 1000; i++) {
            String host = configuration.get("rabbitmq.addr." + i + ".host");
            if (host != null) {
                int port = configuration.getInt("rabbitmq.addr." + i + ".port", DEFAULT_PORT);
                addresses.add(new Address(host, port));
            }
        }

        return addresses.toArray(new Address[addresses.size()]);
    }
}
