package com.altamiracorp.lumify.core.model.videoFrames;

import com.altamiracorp.bigtable.model.RowKey;
import org.apache.commons.lang.StringUtils;

public class VideoFrameRowKey extends RowKey {
    public VideoFrameRowKey(String rowKey) {
        super(rowKey);
    }

    public VideoFrameRowKey(String artifactRowKey, long frameStartTime) {
        super(buildKey(artifactRowKey, frameStartTime));
    }

    private static String buildKey(String artifactRowKey, long frameStartTime) {
        return artifactRowKey
                + ":"
                + StringUtils.leftPad(Long.toString(frameStartTime), 16, '0');
    }

    public Long getTime() {
        return Long.parseLong(this.toString().split(":")[1]);
    }

    public String getArtifactRowKey () {
        return this.toString().split(":")[0];
    }
}
