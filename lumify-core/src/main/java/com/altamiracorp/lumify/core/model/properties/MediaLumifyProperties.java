package com.altamiracorp.lumify.core.model.properties;

import com.altamiracorp.lumify.core.model.properties.types.IdentityLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.types.StreamingLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.types.VideoTranscriptProperty;

/**
 * LumifyProperties for media files (video, images, etc.).
 */
public class MediaLumifyProperties {
    public static final String MIME_TYPE_VIDEO_MP4 = "video/mp4";
    public static final String MIME_TYPE_VIDEO_WEBM = "video/webm";
    public static final String MIME_TYPE_AUDIO_MP3 = "audio/mp3";
    public static final String MIME_TYPE_AUDIO_MP4 = "audio/mp4";
    public static final String MIME_TYPE_AUDIO_OGG = "audio/ogg";

    public static final String METADATA_VIDEO_FRAME_START_TIME = "http://lumify.io#videoFrameStartTime";

    public static final StreamingLumifyProperty VIDEO_MP4 = new StreamingLumifyProperty("http://lumify.io#video-mp4");
    public static final StreamingLumifyProperty VIDEO_WEBM = new StreamingLumifyProperty("http://lumify.io#video-webm");
    public static final StreamingLumifyProperty AUDIO_MP3 = new StreamingLumifyProperty("http://lumify.io#audio-mp3");
    public static final StreamingLumifyProperty AUDIO_MP4 = new StreamingLumifyProperty("http://lumify.io#audio-mp4");
    public static final StreamingLumifyProperty AUDIO_OGG = new StreamingLumifyProperty("http://lumify.io#audio-ogg");

    public static final IdentityLumifyProperty<Long> VIDEO_DURATION = new IdentityLumifyProperty<Long>("http://lumify.io#videoDuration");
    public static final VideoTranscriptProperty VIDEO_TRANSCRIPT = new VideoTranscriptProperty("http://lumify.io#videoTranscript");
    public static final StreamingLumifyProperty RAW_POSTER_FRAME = new StreamingLumifyProperty("http://lumify.io#rawPosterFrame");
    public static final StreamingLumifyProperty VIDEO_PREVIEW_IMAGE = new StreamingLumifyProperty("http://lumify.io#videoPreviewImage");
    public static final StreamingLumifyProperty VIDEO_FRAME = new StreamingLumifyProperty("http://lumify.io#videoFrame");
    public static final StreamingLumifyProperty AUDIO = new StreamingLumifyProperty("http://lumify.io#audio");

    private MediaLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
