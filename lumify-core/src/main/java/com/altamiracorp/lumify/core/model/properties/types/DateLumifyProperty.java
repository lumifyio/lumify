package com.altamiracorp.lumify.core.model.properties.types;

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
