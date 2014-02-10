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

    public static final String ARTIFACT_HIGHLIGHT_QUEUE_NAME = "artifactHighlight";
    public static final String USER_ARTIFACT_HIGHLIGHT_QUEUE_NAME = "userArtifactHighlight";
    public static final String USER_IMAGE_QUEUE_NAME = "userImage";
    public static final String TEXT_QUEUE_NAME = "text";
    public static final String PROCESSED_VIDEO_QUEUE_NAME = "processedVideo";
    public static final String DOCUMENT_QUEUE_NAME = "document";

    public void pushArtifactHighlight(final String artifactGraphVertexId) {
        pushArtifactHighlight(artifactGraphVertexId, FlushFlag.DEFAULT);
    }

    public void pushArtifactHighlight(final String artifactGraphVertexId, FlushFlag flushFlag) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(ARTIFACT_HIGHLIGHT_QUEUE_NAME, flushFlag, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId));
    }

    public void pushText(final String artifactGraphVertexId) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(TEXT_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId));
    }

    public void pushProcessedVideo(final Object artifactGraphVertexId) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(PROCESSED_VIDEO_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId.toString()));
    }

    public void pushUserArtifactHighlight(final String artifactGraphVertexId) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(USER_ARTIFACT_HIGHLIGHT_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId));
    }

    public void pushUserImageQueue(final String graphVertexId) {
        checkNotNull(graphVertexId);
        writeToQueue(USER_IMAGE_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, graphVertexId));
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
