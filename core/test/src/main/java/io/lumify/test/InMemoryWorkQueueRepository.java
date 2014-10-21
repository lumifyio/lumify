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
    private List<GraphPropertyConsumer> graphPropertyConsumers = new ArrayList<GraphPropertyConsumer>();
    private List<LongRunningProcessConsumer> longRunningProcessConsumers = new ArrayList<LongRunningProcessConsumer>();

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
        if (queueName.equals(LONG_RUNNING_PROCESS_QUEUE_NAME)) {
            for (LongRunningProcessConsumer consumer : longRunningProcessConsumers) {
                consumer.longRunningProcessReceived(json);
            }
        } else if (queueName.equals(GRAPH_PROPERTY_QUEUE_NAME)) {
            for (GraphPropertyConsumer graphPropertyConsumer : graphPropertyConsumers) {
                graphPropertyConsumer.graphPropertyReceived(json);
            }
        }
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
        graphPropertyConsumers.add(graphPropertyConsumer);
    }

    @Override
    public void subscribeToLongRunningProcessMessages(LongRunningProcessConsumer longRunningProcessConsumer) {
        longRunningProcessConsumers.add(longRunningProcessConsumer);
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
