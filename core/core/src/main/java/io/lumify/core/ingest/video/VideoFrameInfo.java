package io.lumify.core.ingest.video;

public class VideoFrameInfo {
    public static final String VISIBILITY = "videoFrame";
    private final String propertyKey;
    private final long frameStartTime;
    private final Long frameEndTime;

    public VideoFrameInfo(long frameStartTime, Long frameEndTime, String propertyKey) {
        this.frameStartTime = frameStartTime;
        this.frameEndTime = frameEndTime;
        this.propertyKey = propertyKey;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public long getFrameStartTime() {
        return frameStartTime;
    }

    public Long getFrameEndTime() {
        return frameEndTime;
    }
}
