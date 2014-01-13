package com.altamiracorp.lumify.web.session.model;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;

public class JettySessionData extends ColumnFamily {
    public static final String COLUMN_FAMILY_NAME = "data";

    public JettySessionData() {
        super(COLUMN_FAMILY_NAME);
    }

    public Object getObject(String name) {
        byte[] bytes = Value.toBytes(get(name));
        if (bytes == null) {
            return null;
        }
        return SerializationUtils.deserialize(bytes);
    }

    public JettySessionData setObject(String name, Object object) {
        byte[] bytes = SerializationUtils.serialize((Serializable) object);
        set(name, bytes);
        return this;
    }
}
