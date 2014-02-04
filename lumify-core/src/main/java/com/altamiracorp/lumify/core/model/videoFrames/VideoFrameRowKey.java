package com.altamiracorp.lumify.core.model.videoFrames;

import com.altamiracorp.bigtable.model.RowKey;
import org.apache.commons.lang.StringUtils;

public class VideoFrameRowKey extends RowKey {
    public VideoFrameRowKey(String rowKey) {
        super(rowKey);
    }

    public VideoFrameRowKey(Object artifactGraphVertexId, long frameStartTime) {
        super(buildKey(artifactGraphVertexId, frameStartTime));
    }

    private static String buildKey(Object artifactGraphVertexId, long frameStartTime) {
        return artifactGraphVertexId.toString()
                + ":"
                + StringUtils.leftPad(Long.toString(frameStartTime), 16, '0');
    }

    public Long getTime() {
        return Long.parseLong(this.toString().split(":")[1]);
    }
}
