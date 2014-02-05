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
    /**
     * The MP4 Video type.
     */
    public static final String VIDEO_TYPE_MP4 = "video/mp4";

    /**
     * The WebM Video type.
     */
    public static final String VIDEO_TYPE_WEBM = "video/webm";

    /**
     * The video property format.
     */
    private static final String VIDEO_KEY_FORMAT = "_video-%s";

    /**
     * The video size property format.
     */
    private static final String VIDEO_SIZE_KEY_FORMAT = "_videoSize-%s";

    /**
     * The map of video types to video properties.
     */
    private static final Map<String, StreamingLumifyProperty> VIDEO_PROPERTY_CACHE = new HashMap<String, StreamingLumifyProperty>();

    /**
     * Get the video property for the requested video type.
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
     * The map of video types to video size properties.
     */
    private static final Map<String, IdentityLumifyProperty<Long>> VIDEO_SIZE_PROPERTY_CACHE =
            new HashMap<String, IdentityLumifyProperty<Long>>();

    /**
     * Get the video size property for the requested video type.
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
