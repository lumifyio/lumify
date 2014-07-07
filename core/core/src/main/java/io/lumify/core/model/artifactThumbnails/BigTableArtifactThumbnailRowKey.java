package io.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.RowKey;
import org.apache.commons.lang.StringUtils;

public class BigTableArtifactThumbnailRowKey extends RowKey {
    public BigTableArtifactThumbnailRowKey(String rowKey) {
        super(rowKey);
    }

    public BigTableArtifactThumbnailRowKey(Object artifactVertexId, String thumbnailType, int width, int height) {
        super(buildKey(artifactVertexId, thumbnailType, width, height));
    }

    private static String buildKey(Object artifactVertexId, String thumbnailType, int width, int height) {
        return artifactVertexId.toString()
                + ":" + thumbnailType
                + ":" + StringUtils.leftPad(Integer.toString(width), 8, '0')
                + ":" + StringUtils.leftPad(Integer.toString(height), 8, '0');
    }
}
