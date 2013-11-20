package com.altamiracorp.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.RowKey;
import org.apache.commons.lang.StringUtils;

public class ArtifactThumbnailRowKey extends RowKey {
    public ArtifactThumbnailRowKey(String rowKey) {
        super(rowKey);
    }

    public ArtifactThumbnailRowKey(String artifactRowKey, String thumbnailType, int width, int height) {
        super(buildKey(artifactRowKey, thumbnailType, width, height));
    }

    private static String buildKey(String artifactRowKey, String thumbnailType, int width, int height) {
        return artifactRowKey
                + ":" + thumbnailType
                + ":" + StringUtils.leftPad(Integer.toString(width), 8, '0')
                + ":" + StringUtils.leftPad(Integer.toString(height), 8, '0');
    }
}
