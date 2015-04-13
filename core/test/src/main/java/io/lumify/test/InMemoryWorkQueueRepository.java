package io.lumify.test;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.WorkerSpout;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerTuple;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import org.json.JSONObject;
import org.securegraph.Graph;

import java.util.*;

public class InMemoryWorkQueueRepository extends WorkQueueRepository {

    private static Map<String, Queue<JSONObject>> queues = new HashMap<String, Queue<JSONObject>>();
    private List<BroadcastConsumer> broadcastConsumers = new ArrayList<BroadcastConsumer>();

    @Inject
    public InMemoryWorkQueueRepository(Graph graph, Configuration configuration) {
        super(graph, configuration);
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
        addToQueue(queueName, json);
    }

    public void addToQueue(String queueName, JSONObject json) {
        final Queue<JSONObject> queue = getQueue(queueName);
        synchronized (queue) {
            queue.add(json);
            queue.notifyAll();
        }
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
    public LongRunningProcessMessage getNextLongRunningProcessMessage() {
        final Queue<JSONObject> longRunningProcessMessageQueue = getQueue(LONG_RUNNING_PROCESS_QUEUE_NAME);
        synchronized (longRunningProcessMessageQueue) {
            while (true) {
                if (longRunningProcessMessageQueue.size() > 0) {
                    JSONObject message = longRunningProcessMessageQueue.remove();
                    return new InMemoryLongRunningProcessMessage(message);
                }
                try {
                    longRunningProcessMessageQueue.wait();
                } catch (InterruptedException ex) {
                    throw new LumifyException("Could not get next long running process message", ex);
                }
            }
        }
    }

    @Override
    public WorkerSpout createWorkerSpout() {
        final Queue<JSONObject> queue = getQueue(GRAPH_PROPERTY_QUEUE_NAME);
        return new WorkerSpout() {
            @Override
            public GraphPropertyWorkerTuple nextTuple() throws Exception {
                JSONObject entry = queue.poll();
                if (entry == null) {
                    return null;
                }
                return new GraphPropertyWorkerTuple("", entry);
            }
        };
    }

    private class InMemoryLongRunningProcessMessage extends LongRunningProcessMessage {
        public InMemoryLongRunningProcessMessage(JSONObject message) {
            super(message);
        }

        @Override
        public void complete(Throwable ex) {
            if (ex != null) {
                LOGGER.error("Failed to process long running process message ", ex);
                addToQueue(LONG_RUNNING_PROCESS_QUEUE_NAME, getMessage());
            }
        }
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
