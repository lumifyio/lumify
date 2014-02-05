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

import java.util.Date;

/**
 * A LumifyProperty that converts Dates to an appropriate value for
 * storage in SecureGraph.
 */
public class DateLumifyProperty extends IdentityLumifyProperty<Date> {
    /**
     * Create a new DateLumifyProperty.
     * @param key the property key
     */
    public DateLumifyProperty(String key) {
        super(key);
    }
}
