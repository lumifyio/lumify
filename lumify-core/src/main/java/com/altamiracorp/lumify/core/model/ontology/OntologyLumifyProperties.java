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

import com.altamiracorp.lumify.core.model.properties.BooleanLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.TextLumifyProperty;
import com.altamiracorp.securegraph.TextIndexHint;

/**
 * LumifyProperty values used for storage of Ontology concepts.
 */
public final class OntologyLumifyProperties {
    public static final TextLumifyProperty CONCEPT_TYPE = new TextLumifyProperty("http://lumify.io#conceptType", TextIndexHint.EXACT_MATCH);

    public static final TextLumifyProperty DATA_TYPE = new TextLumifyProperty("http://lumify.io#dataType", TextIndexHint.EXACT_MATCH);

    public static final BooleanLumifyProperty USER_VISIBLE = new BooleanLumifyProperty("http://lumify.io#userVisible");

    public static final TextLumifyProperty ONTOLOGY_TITLE = new TextLumifyProperty("http://lumify.io#ontologyTitle", TextIndexHint.EXACT_MATCH);

    public static final TextLumifyProperty DISPLAY_TYPE = new TextLumifyProperty("http://lumify.io#displayType", TextIndexHint.EXACT_MATCH);

    public static final TextLumifyProperty COLOR = new TextLumifyProperty("http://lumify.io#color", TextIndexHint.NONE);

    private OntologyLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
