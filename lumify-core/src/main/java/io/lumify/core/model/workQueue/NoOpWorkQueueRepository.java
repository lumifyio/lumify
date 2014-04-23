package io.lumify.core.model.workQueue;

import com.altamiracorp.bigtable.model.FlushFlag;
import io.lumify.core.config.Configuration;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import org.json.JSONObject;

public class NoOpWorkQueueRepository extends WorkQueueRepository {
    @Inject
    protected NoOpWorkQueueRepository(Graph graph) {
        super(graph);
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Object createSpout(Configuration configuration, String queueName) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void flush() {
        throw new RuntimeException("not supported");
    }

    @Override
    public void format() {
        throw new RuntimeException("not supported");
    }

    @Override
    public void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer) {

    }
}
