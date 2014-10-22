package io.lumify.test;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import org.json.JSONObject;
import org.securegraph.Graph;

import java.util.*;

public class InMemoryWorkQueueRepository extends WorkQueueRepository {

    private static Map<String, Queue<JSONObject>> queues = new HashMap<String, Queue<JSONObject>>();
    private List<BroadcastConsumer> broadcastConsumers = new ArrayList<BroadcastConsumer>();

    @Inject
    public InMemoryWorkQueueRepository(Graph graph) {
        super(graph);
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        for (BroadcastConsumer consumer : broadcastConsumers) {
            consumer.broadcastReceived(json);
        }
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json) {
        LOGGER.debug("push on queue: %s: %s", queueName, json);
        getQueue(queueName).add(json);
    }

    @Override
    public Object createSpout(Configuration configuration, String queueName) {
        throw new UnsupportedOperationException("Spout creation is not supported");
    }

    @Override
    public void flush() {

    }

    @Override
    public void format() {
        clearQueue();
    }

    @Override
    public void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer) {
        broadcastConsumers.add(broadcastConsumer);
    }

    @Override
    public void subscribeToGraphPropertyMessages(GraphPropertyConsumer graphPropertyConsumer) {
        throw new UnsupportedOperationException("subscribing to graph property messages is not supported");
    }

    public static void clearQueue() {
        queues.clear();
    }

    public static Queue<JSONObject> getQueue(String queueName) {
        Queue<JSONObject> queue = queues.get(queueName);
        if (queue == null) {
            queue = new LinkedList<JSONObject>();
            queues.put(queueName, queue);
        }
        return queue;
    }
}
