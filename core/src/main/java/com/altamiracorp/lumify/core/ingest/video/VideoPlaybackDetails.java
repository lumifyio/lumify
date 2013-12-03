package com.altamiracorp.lumify.core.ingest.video;

import java.io.InputStream;

public class VideoPlaybackDetails {
    private final long videoFileSize;
    private final InputStream videoStream;

    public VideoPlaybackDetails(final InputStream stream, final long fileSize) {
        videoStream = stream;
        videoFileSize = fileSize;
    }


    public long getVideoFileSize() {
        return videoFileSize;
    }

    public InputStream getVideoStream() {
        return videoStream;
    }
}
