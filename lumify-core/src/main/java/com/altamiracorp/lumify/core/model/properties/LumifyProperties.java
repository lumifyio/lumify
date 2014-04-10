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
 * General LumifyProperty instances that apply across Ontology and data vertices.
 */
public class LumifyProperties {
    public static final TextLumifyProperty DISPLAY_NAME = TextLumifyProperty.all("http://lumify.io#displayName");
    public static final TextLumifyProperty PROCESS = TextLumifyProperty.all("http://lumify.io#process");
    public static final TextLumifyProperty ROW_KEY = new TextLumifyProperty("http://lumify.io#rowKey", TextIndexHint.EXACT_MATCH);
    public static final StreamingLumifyProperty GLYPH_ICON = new StreamingLumifyProperty("http://lumify.io#glyphIcon");
    public static final StreamingLumifyProperty MAP_GLYPH_ICON = new StreamingLumifyProperty("http://lumify.io#mapGlyphIcon");
    public static final TextLumifyProperty TITLE = TextLumifyProperty.all("http://lumify.io#title");
    public static final DateLumifyProperty MODIFIED_DATE = new DateLumifyProperty("http://lumify.io#modifiedDate");
    public static final TextLumifyProperty MODIFIED_BY = new TextLumifyProperty("http://lumify.io#modifiedBy", TextIndexHint.EXACT_MATCH);

    private LumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
