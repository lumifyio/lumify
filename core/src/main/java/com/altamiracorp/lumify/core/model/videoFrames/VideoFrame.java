package com.altamiracorp.lumify.core.model.videoFrames;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;

public class VideoFrame extends Row<VideoFrameRowKey> {
    public static final String TABLE_NAME = "atc_videoFrame";

    public VideoFrame(VideoFrameRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public VideoFrame(RowKey rowKey) {
        super(TABLE_NAME, new VideoFrameRowKey(rowKey.toString()));
    }

    public VideoFrameMetadata getMetadata() {
        VideoFrameMetadata videoFrameMetadata = get(VideoFrameMetadata.NAME);
        if (videoFrameMetadata == null) {
            addColumnFamily(new VideoFrameMetadata());
        }
        return get(VideoFrameMetadata.NAME);
    }

    public VideoFrameDetectedObjects getDetectedObjects() {
        VideoFrameDetectedObjects videoFrameDetectedObjects = get(VideoFrameDetectedObjects.NAME);
        if (videoFrameDetectedObjects == null) {
            addColumnFamily(new VideoFrameDetectedObjects());
        }
        return get(VideoFrameDetectedObjects.NAME);
    }
}
