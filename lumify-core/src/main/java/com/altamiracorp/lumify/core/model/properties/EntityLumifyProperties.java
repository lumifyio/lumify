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

import com.altamiracorp.securegraph.type.GeoPoint;

/**
 * LumifyProperties that apply to both raw and resolved entities stored in the Lumify system.
 */
public class EntityLumifyProperties {
    /**
     * The geo location of the entity.
     */
    public static final IdentityLumifyProperty<GeoPoint> GEO_LOCATION = new IdentityLumifyProperty<GeoPoint>("geoLocation");

    /**
     * The geo location description.
     */
    public static final TextLumifyProperty GEO_LOCATION_DESCRIPTION = TextLumifyProperty.all("_geoLocationDescription");

    /**
     * The ingest source of the entity.
     */
    public static final TextLumifyProperty SOURCE = TextLumifyProperty.all("_source");

    /**
     * Utility class consructor.
     */
    private EntityLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
