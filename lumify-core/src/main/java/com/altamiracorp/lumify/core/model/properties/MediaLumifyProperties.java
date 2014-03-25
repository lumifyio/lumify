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

import java.util.HashMap;
import java.util.Map;

/**
 * LumifyProperties for media files (video, images, etc.).
 */
public class MediaLumifyProperties {
    public static final String VIDEO_TYPE_MP4 = "video/mp4";
    public static final String VIDEO_TYPE_WEBM = "video/webm";
    public static final String AUDIO_TYPE_MP4 = "audio/mp4";
    public static final String AUDIO_TYPE_OGG = "audio/ogg";

    private static final String AUDIO_KEY_FORMAT = "_audio-%s";
    private static final String VIDEO_KEY_FORMAT = "_video-%s";

    private static final String VIDEO_SIZE_KEY_FORMAT = "_videoSize-%s";
    private static final String AUDIO_SIZE_KEY_FORMAT = "_audioSize-%s";

    private static final Map<String, IdentityLumifyProperty<Long>> VIDEO_SIZE_PROPERTY_CACHE = new HashMap<String, IdentityLumifyProperty<Long>>();
    private static final Map<String, StreamingLumifyProperty> VIDEO_PROPERTY_CACHE = new HashMap<String, StreamingLumifyProperty>();
    private static final Map<String, IdentityLumifyProperty<Long>> AUDIO_SIZE_PROPERTY_CACHE = new HashMap<String, IdentityLumifyProperty<Long>>();
    private static final Map<String, StreamingLumifyProperty> AUDIO_PROPERTY_CACHE = new HashMap<String, StreamingLumifyProperty>();

    /**
     * Get the video property for the requested video type.
     *
     * @param videoType the type of video (e.g. video/mp4)
     * @return a LumifyProperty for setting/retrieving the requested video property
     */
    public static synchronized StreamingLumifyProperty getVideoProperty(final String videoType) {
        StreamingLumifyProperty prop = VIDEO_PROPERTY_CACHE.get(videoType);
        if (prop == null) {
            prop = new StreamingLumifyProperty(String.format(VIDEO_KEY_FORMAT, videoType));
            VIDEO_PROPERTY_CACHE.put(videoType, prop);
        }
        return prop;
    }

    /**
     * Get the audio property for the requested audio type.
     *
     * @param audioType the type of video (e.g. audio/ogg)
     * @return a LumifyProperty for setting/retrieving the requested audio property
     */
    public static synchronized StreamingLumifyProperty getAudioProperty(final String audioType) {
        StreamingLumifyProperty prop = AUDIO_PROPERTY_CACHE.get(audioType);
        if (prop == null) {
            prop = new StreamingLumifyProperty(String.format(AUDIO_KEY_FORMAT, audioType));
            AUDIO_PROPERTY_CACHE.put(audioType, prop);
        }
        return prop;
    }

    /**
     * Get the video size property for the requested video type.
     *
     * @param videoType the type of video (e.g. video/mp4)
     * @return a LumifyProperty for setting/retrieving the requested video size property
     */
    public static synchronized IdentityLumifyProperty<Long> getVideoSizeProperty(final String videoType) {
        IdentityLumifyProperty<Long> prop = VIDEO_SIZE_PROPERTY_CACHE.get(videoType);
        if (prop == null) {
            prop = new IdentityLumifyProperty<Long>(String.format(VIDEO_SIZE_KEY_FORMAT, videoType));
            VIDEO_SIZE_PROPERTY_CACHE.put(videoType, prop);
        }
        return prop;
    }

    public static synchronized IdentityLumifyProperty<Long> getAudioSizeProperty(final String audioType) {
        IdentityLumifyProperty<Long> prop = AUDIO_SIZE_PROPERTY_CACHE.get(audioType);
        if (prop == null) {
            prop = new IdentityLumifyProperty<Long>(String.format(AUDIO_SIZE_KEY_FORMAT, audioType));
            AUDIO_SIZE_PROPERTY_CACHE.put(audioType, prop);
        }
        return prop;
    }

    /**
     * The video duration property.
     */
    public static final IdentityLumifyProperty<Long> VIDEO_DURATION = new IdentityLumifyProperty<Long>("_videoDuration");

    /**
     * The video transcript property.
     */
    public static final TextLumifyProperty VIDEO_TRANSCRIPT = TextLumifyProperty.all("_videoTranscript");

    /**
     * The raw poster frame property.
     */
    public static final StreamingLumifyProperty RAW_POSTER_FRAME = new StreamingLumifyProperty("_rawPosterFrame");

    /**
     * The video preview image property.
     */
    public static final StreamingLumifyProperty VIDEO_PREVIEW_IMAGE = new StreamingLumifyProperty("_videoPreviewImage");

    /**
     * The audio property.
     */
    public static final StreamingLumifyProperty AUDIO = new StreamingLumifyProperty("_audio");

    /**
     * Utility class constructor.
     */
    private MediaLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
