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

import com.altamiracorp.securegraph.TextIndexHint;

/**
 * LumifyProperties specific to Raw entities (e.g. documents, images, video, etc.).
 */
public class RawLumifyProperties {
    /**
     * The publication date.
     */
    public static final DateLumifyProperty PUBLISHED_DATE = new DateLumifyProperty("publishedDate");

    /**
     * The creation date.
     */
    public static final DateLumifyProperty CREATE_DATE = new DateLumifyProperty("_createDate");

    /**
     * The filename property.
     */
    public static final TextLumifyProperty FILE_NAME = TextLumifyProperty.all("_fileName");

    /**
     * The filename extension property.
     */
    public static final TextLumifyProperty FILE_NAME_EXTENSION = new TextLumifyProperty("_fileNameExtension", TextIndexHint.EXACT_MATCH);

    /**
     * The mime type.
     */
    public static final TextLumifyProperty MIME_TYPE = TextLumifyProperty.all("_mimeType");

    /**
     * The author property.
     */
    public static final TextLumifyProperty AUTHOR = TextLumifyProperty.all("author");

    /**
     * The raw property.
     */
    public static final StreamingLumifyProperty RAW = new StreamingLumifyProperty("_raw");

    /**
     * The text property.
     */
    public static final StreamingLumifyProperty TEXT = new StreamingLumifyProperty("_text");

    /**
     * The mapping JSON property.
     */
    public static final TextLumifyProperty MAPPING_JSON = new TextLumifyProperty("_mappingJson", TextIndexHint.NONE);

    /**
     * The detected objects property.
     */
    public static final TextLumifyProperty DETECTED_OBJECTS_JSON = new TextLumifyProperty("_detectedObjects", TextIndexHint.NONE);

    /**
     * Utility class constructor.
     */
    private RawLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
