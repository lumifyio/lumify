package com.altamiracorp.lumify.core.model.workQueue;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkQueueRepository {
    protected static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkQueueRepository.class);
    public static final String GRAPH_PROPERTY_QUEUE_NAME = "graphProperty";
    private final Graph graph;

    @Inject
    protected WorkQueueRepository(Graph graph) {
        this.graph = graph;
    }

    public void pushGraphPropertyQueue(final Object graphVertexId, final String propertyKey, final String propertyName) {
        getGraph().flush();

        checkNotNull(graphVertexId);
        checkNotNull(propertyKey);
        checkNotNull(propertyName);
        JSONObject data = new JSONObject();
        data.put("graphVertexId", graphVertexId);
        data.put("propertyKey", propertyKey);
        data.put("propertyName", propertyName);
        pushOnQueue(GRAPH_PROPERTY_QUEUE_NAME, FlushFlag.DEFAULT, data);
    }

    public abstract void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, String... extra);

    public void init(Map map) {

    }

    // TODO this is pretty awful but returning backtype.storm.topology.IRichSpout causes a dependency hell problem because it requires storm jar
    //      one possibility would be to return a custom type but this just pushes the problem
    public abstract Object createSpout(Configuration configuration, String queueName, Long queueStartOffsetTime);

    public abstract void flush();

    public abstract void format();

    public Graph getGraph() {
        return graph;
    }
}
