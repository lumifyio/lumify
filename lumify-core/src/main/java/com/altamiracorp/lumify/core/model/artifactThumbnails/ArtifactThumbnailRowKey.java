package com.altamiracorp.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.RowKey;
import org.apache.commons.lang.StringUtils;

public class ArtifactThumbnailRowKey extends RowKey {
    public ArtifactThumbnailRowKey(String rowKey) {
        super(rowKey);
    }

    public ArtifactThumbnailRowKey(Object artifactVertexId, String thumbnailType, int width, int height) {
        super(buildKey(artifactVertexId, thumbnailType, width, height));
    }

    private static String buildKey(Object artifactVertexId, String thumbnailType, int width, int height) {
        return artifactVertexId.toString()
                + ":" + thumbnailType
                + ":" + StringUtils.leftPad(Integer.toString(width), 8, '0')
                + ":" + StringUtils.leftPad(Integer.toString(height), 8, '0');
    }
}
