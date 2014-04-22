package com.altamiracorp.lumify.core.model.workQueue;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Element;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONObject;

public class NoOpWorkQueueRepository extends WorkQueueRepository {
    @Inject
    protected NoOpWorkQueueRepository(Graph graph) {
        super(graph);
    }

    @Override
    protected void broadcastPropertyChange(Edge edge, String propertyKey, String propertyName) {
        throw new RuntimeException("not supported");
    }

    @Override
    protected void broadcastPropertyChange(Vertex graphVertex, String propertyKey, String propertyName) {
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
