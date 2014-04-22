package com.altamiracorp.lumify.model.rabbitmq;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;

public class RabbitMQWorkQueueRepository extends WorkQueueRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RabbitMQWorkQueueRepository.class);
    private final Channel channel;
    private HashSet<String> declaredQueues = new HashSet<String>();

    @Inject
    public RabbitMQWorkQueueRepository(Graph graph, Configuration configuration) throws IOException {
        super(graph);
        this.channel = RabbitMQUtils.openChannel(configuration);
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
        return new RabbitMQWorkQueueSpout(queueName);
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
}
