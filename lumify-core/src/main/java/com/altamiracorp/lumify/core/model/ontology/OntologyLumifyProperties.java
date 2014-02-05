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

package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.properties.TextLumifyProperty;
import com.altamiracorp.securegraph.TextIndexHint;

/**
 * LumifyProperty values used for storage of Ontology concepts.
 */
public final class OntologyLumifyProperties {
    /**
     * The Concept Type property.
     */
    public static final TextLumifyProperty CONCEPT_TYPE = new TextLumifyProperty("_conceptType", TextIndexHint.EXACT_MATCH);

    /**
     * The Data Type property.
     */
    public static final TextLumifyProperty DATA_TYPE = new TextLumifyProperty("_dataType", TextIndexHint.EXACT_MATCH);

    /**
     * The Ontology Title property.
     */
    public static final TextLumifyProperty ONTOLOGY_TITLE = new TextLumifyProperty("ontologyTitle", TextIndexHint.EXACT_MATCH);

    /**
     * The Color property.
     */
    public static final TextLumifyProperty COLOR = new TextLumifyProperty("_color", TextIndexHint.NONE);

    /**
     * Utility class. No construction allowed.
     */
    private OntologyLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
