package com.altamiracorp.lumify.core.model.workQueue;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkQueueRepository {
    protected static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkQueueRepository.class);

    public static final String KEY_GRAPH_VERTEX_ID = "graphVertexId";

    public static final String USER_IMAGE_QUEUE_NAME = "userImage";
    public static final String TEXT_QUEUE_NAME = "text";
    public static final String PROCESSED_VIDEO_QUEUE_NAME = "processedVideo";
    public static final String DOCUMENT_QUEUE_NAME = "document";
    public static final String GRAPH_PROPERTY_QUEUE_NAME = "graphProperty";

    public void pushText(final String artifactGraphVertexId) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(TEXT_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId));
    }

    public void pushProcessedVideo(final Object artifactGraphVertexId) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(PROCESSED_VIDEO_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId.toString()));
    }

    public void pushUserImageQueue(final String graphVertexId) {
        checkNotNull(graphVertexId);
        writeToQueue(USER_IMAGE_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, graphVertexId));
    }

    public void pushGraphPropertyQueue(final Object graphVertexId, final String propertyKey, final String propertyName) {
        checkNotNull(graphVertexId);
        checkNotNull(propertyKey);
        checkNotNull(propertyName);
        JSONObject data = new JSONObject();
        data.put("graphVertexId", graphVertexId);
        data.put("propertyKey", propertyKey);
        data.put("propertyName", propertyKey);
        pushOnQueue(GRAPH_PROPERTY_QUEUE_NAME, FlushFlag.DEFAULT, data);
    }

    private void writeToQueue(final String queueName, FlushFlag flushFlag, final Map<String, String> content) {
        JSONObject data = contentToJson(content);
        LOGGER.debug("Writing data: %s to queue [%s]", data, queueName);
        pushOnQueue(queueName, flushFlag, data);
    }

    public static JSONObject contentToJson(final Map<String, String> content) {
        final JSONObject data = new JSONObject();
        for (final Map.Entry<String, String> entry : content.entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }
        return data;
    }

    public abstract void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, String... extra);

    public void init(Map map) {

    }

    // TODO this is pretty awful but returning backtype.storm.topology.IRichSpout causes a dependency hell problem because it requires storm jar
    //      one possibility would be to return a custom type but this just pushes the problem
    public abstract Object createSpout(Configuration configuration, String queueName, Long queueStartOffsetTime);

    public abstract void flush();

    public abstract void format();
}
