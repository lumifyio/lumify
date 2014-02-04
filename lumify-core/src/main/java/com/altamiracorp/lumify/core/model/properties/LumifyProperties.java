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
 * General LumifyProperty instances that apply across Ontology and data vertices.
 */
public class LumifyProperties {
    /**
     * The display name property.
     */
    public static final LumifyProperty<String, String> DISPLAY_NAME = new IdentityLumifyProperty<String>("displayName");

    /**
     * The display type property.
     */
    public static final LumifyProperty<String, String> DISPLAY_TYPE = new IdentityLumifyProperty<String>("displayType");

    /**
     * The process property.
     */
    public static final LumifyProperty<String, String> PROCESS = new IdentityLumifyProperty<String>("_process");

    /**
     * The row key property.
     */
    public static final LumifyProperty<String, String> ROW_KEY = new IdentityLumifyProperty<String>("_rowKey");

    /**
     * The glyph icon property.
     */
    public static final LumifyProperty<String, String> GLYPH_ICON = new IdentityLumifyProperty<String>("_glyphIcon");

    /**
     * The map glyph icon property.
     */
    public static final LumifyProperty<String, String> MAP_GLYPH_ICON = new IdentityLumifyProperty<String>("_mapGlyphIcon");

    /**
     * Utility class constructor.
     */
    private LumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
