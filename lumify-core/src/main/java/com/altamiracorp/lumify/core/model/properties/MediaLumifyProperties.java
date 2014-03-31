/*
 * Copyright 2014 Altamira Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.altamiracorp.lumify.core.model.properties;

/**
 * LumifyProperties for media files (video, images, etc.).
 */
public class MediaLumifyProperties {
    public static final String MIME_TYPE_VIDEO_MP4 = "video/mp4";
    public static final String MIME_TYPE_VIDEO_WEBM = "video/webm";
    public static final String MIME_TYPE_AUDIO_MP4 = "audio/mp4";
    public static final String MIME_TYPE_AUDIO_OGG = "audio/ogg";

    public static final String METADATA_VIDEO_FRAME_START_TIME = "http://lumify.io#videoFrameStartTime";

    public static final StreamingLumifyProperty VIDEO_MP4 = new StreamingLumifyProperty("http://lumify.io#video-mp4");
    public static final StreamingLumifyProperty VIDEO_WEBM = new StreamingLumifyProperty("http://lumify.io#video-webm");
    public static final StreamingLumifyProperty AUDIO_MP4 = new StreamingLumifyProperty("http://lumify.io#audio-mp4");
    public static final StreamingLumifyProperty AUDIO_OGG = new StreamingLumifyProperty("http://lumify.io#audio-ogg");

    public static final IdentityLumifyProperty<Long> VIDEO_DURATION = new IdentityLumifyProperty<Long>("http://lumify.io#videoDuration");
    public static final TextLumifyProperty VIDEO_TRANSCRIPT = TextLumifyProperty.all("http://lumify.io#videoTranscript");
    public static final StreamingLumifyProperty RAW_POSTER_FRAME = new StreamingLumifyProperty("http://lumify.io#rawPosterFrame");
    public static final StreamingLumifyProperty VIDEO_PREVIEW_IMAGE = new StreamingLumifyProperty("http://lumify.io#videoPreviewImage");
    public static final StreamingLumifyProperty VIDEO_FRAME = new StreamingLumifyProperty("http://lumify.io#videoFrame");
    public static final StreamingLumifyProperty AUDIO = new StreamingLumifyProperty("http://lumify.io#audio");

    private MediaLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
