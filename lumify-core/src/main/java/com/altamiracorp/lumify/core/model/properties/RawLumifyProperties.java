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
    public static final DateLumifyProperty PUBLISHED_DATE = new DateLumifyProperty("http://lumify.io#publishedDate");
    public static final DateLumifyProperty CREATE_DATE = new DateLumifyProperty("http://lumify.io#createDate");
    public static final TextLumifyProperty FILE_NAME = TextLumifyProperty.all("http://lumify.io#fileName");
    public static final TextLumifyProperty FILE_NAME_EXTENSION = new TextLumifyProperty("http://lumify.io#fileNameExtension", TextIndexHint.EXACT_MATCH);
    public static final TextLumifyProperty MIME_TYPE = TextLumifyProperty.all("http://lumify.io#mimeType");
    public static final TextLumifyProperty AUTHOR = TextLumifyProperty.all("http://lumify.io#author");
    public static final StreamingLumifyProperty RAW = new StreamingLumifyProperty("http://lumify.io#raw");
    public static final StreamingLumifyProperty TEXT = new StreamingLumifyProperty("http://lumify.io#text");

    private RawLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
