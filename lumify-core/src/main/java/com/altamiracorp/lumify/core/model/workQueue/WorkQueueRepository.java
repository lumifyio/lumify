package com.altamiracorp.lumify.core.model.workQueue;

import backtype.storm.topology.IRichSpout;
import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkQueueRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkQueueRepository.class);

    private static final String KEY_ARTIFACT_ROWKEY = "artifactRowKey";
    private static final String KEY_GRAPH_VERTEX_ID = "graphVertexId";

    public static final String ARTIFACT_HIGHLIGHT_QUEUE_NAME = "artifactHighlight";
    public static final String USER_ARTIFACT_HIGHLIGHT_QUEUE_NAME = "userArtifactHighlight";
    public static final String USER_IMAGE_QUEUE_NAME = "userImage";
    public static final String TEXT_QUEUE_NAME = "text";
    public static final String PROCESSED_VIDEO_QUEUE_NAME = "processedVideo";
    public static final String DOCUMENT_QUEUE_NAME = "document";

    public void pushArtifactHighlight(final String artifactGraphVertexId) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(ARTIFACT_HIGHLIGHT_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId));
    }

    public void pushText(final String artifactGraphVertexId) {
        checkNotNull(artifactGraphVertexId);
        writeToQueue(TEXT_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, artifactGraphVertexId));
    }

    public void pushProcessedVideo(final String artifactRowKey) {
        checkNotNull(artifactRowKey);
        writeToQueue(PROCESSED_VIDEO_QUEUE_NAME, FlushFlag.DEFAULT, ImmutableMap.<String, String>of(KEY_ARTIFACT_ROWKEY, artifactRowKey));
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
        final JSONObject data = new JSONObject();

        for (final Map.Entry<String, String> entry : content.entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }

        LOGGER.debug("Writing data: %s to queue [%s]", data, queueName);
        pushOnQueue(queueName, flushFlag, data);
    }

    public abstract void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, String... extra);

    public void init(Map map) {

    }

    public abstract IRichSpout createSpout(Configuration configuration, String queueName, Long queueStartOffsetTime);

    public abstract void flush();

    public abstract void format();
}
