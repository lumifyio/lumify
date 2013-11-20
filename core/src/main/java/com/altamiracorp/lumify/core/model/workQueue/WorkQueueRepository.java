package com.altamiracorp.lumify.core.model.workQueue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public abstract class WorkQueueRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkQueueRepository.class);

    private static final String KEY_ARTIFACT_ROWKEY = "artifactRowKey";
    private static final String KEY_GRAPH_VERTEX_ID = "graphVertexId";

    public static final String ARTIFACT_HIGHLIGHT_QUEUE_NAME = "artifactHighlight";
    public static final String TEXT_QUEUE_NAME = "text";
    public static final String PROCESSED_VIDEO_QUEUE_NAME = "processedVideo";

    public void pushArtifactHighlight(final String artifactGraphVertexId) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(ARTIFACT_HIGHLIGHT_QUEUE_NAME, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId));
    }

    public void pushText(final String artifactGraphVertexId) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(TEXT_QUEUE_NAME, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId));
    }

    public void pushProcessedVideo(final String artifactRowKey) {
        checkNotNull(artifactRowKey);
        writeToQueue(PROCESSED_VIDEO_QUEUE_NAME, ImmutableMap.<String, String>of(KEY_ARTIFACT_ROWKEY, artifactRowKey));
    }

    private void writeToQueue(final String queueName, final Map<String, String> content) {
        final JSONObject data = new JSONObject();

        for(final Map.Entry<String, String> entry : content.entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }

        LOGGER.debug(String.format("Writing data: %s to queue [%s]", data, queueName));
        pushOnQueue(queueName, data);
    }

    protected abstract void pushOnQueue(String queueName, JSONObject json, String... extra);
}
