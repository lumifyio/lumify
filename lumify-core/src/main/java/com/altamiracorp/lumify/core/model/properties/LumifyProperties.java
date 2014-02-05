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
 *//*
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
    /**
     * The display name property.
     */
    public static final TextLumifyProperty DISPLAY_NAME = TextLumifyProperty.all("displayName");

    /**
     * The display type property.
     */
    public static final TextLumifyProperty DISPLAY_TYPE = new TextLumifyProperty("displayType", TextIndexHint.EXACT_MATCH);

    /**
     * The process property.
     */
    public static final TextLumifyProperty PROCESS = TextLumifyProperty.all("_process");

    /**
     * The row key property.
     */
    public static final TextLumifyProperty ROW_KEY = new TextLumifyProperty("_rowKey", TextIndexHint.EXACT_MATCH);

    /**
     * The glyph icon property.
     */
    public static final StreamingLumifyProperty GLYPH_ICON = new StreamingLumifyProperty("_glyphIcon");

    /**
     * The map glyph icon property.
     */
    public static final StreamingLumifyProperty MAP_GLYPH_ICON = new StreamingLumifyProperty("_mapGlyphIcon");

    /**
     * The title property.
     */
    public static final TextLumifyProperty TITLE = TextLumifyProperty.all("title");

    /**
     * Utility class constructor.
     */
    private LumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
