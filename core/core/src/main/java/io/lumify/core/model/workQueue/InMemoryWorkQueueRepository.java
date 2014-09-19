package io.lumify.core.model.workQueue;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import org.json.JSONObject;
import org.securegraph.Graph;

import java.util.*;

public class InMemoryWorkQueueRepository extends WorkQueueRepository {

    private Map<String, Queue<JSONObject>> queues = new HashMap<String, Queue<JSONObject>>();
    private List<BroadcastConsumer> consumers = new ArrayList<BroadcastConsumer>();

    @Inject
    public InMemoryWorkQueueRepository(Graph graph) {
        super(graph);
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        for (BroadcastConsumer consumer : consumers) {
            consumer.broadcastReceived(json);
        }
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json) {
        Queue queue = queues.get(queueName);
        if (queue == null) {
            queue = new LinkedList();
            queues.put(queueName, queue);
        }
        queue.add(json);
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
        queues.clear();
    }

    @Override
    public void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer) {
        consumers.add(broadcastConsumer);
    }

    public Queue<JSONObject> getQueue(String queueName) {
        return queues.get(queueName);
    }
}
