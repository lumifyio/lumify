package io.lumify.core.ingest.video;

public class VideoFrameInfo {
    private final String propertyKey;
    private final long frameStartTime;

    public VideoFrameInfo(long frameStartTime, String propertyKey) {
        this.frameStartTime = frameStartTime;
        this.propertyKey = propertyKey;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public long getFrameStartTime() {
        return frameStartTime;
    }
}
