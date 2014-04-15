package com.altamiracorp.lumify.core.model.workQueue;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import org.json.JSONObject;

public class NoOpWorkQueueRepository extends WorkQueueRepository {
    @Inject
    protected NoOpWorkQueueRepository(Graph graph) {
        super(graph);
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, String... extra) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Object createSpout(Configuration configuration, String queueName, Long queueStartOffsetTime) {
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
}
