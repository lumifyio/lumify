package com.altamiracorp.lumify.core.model.geoNames;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;

public class GeoName extends Row<GeoNameRowKey> {
    public static final String TABLE_NAME = "atc_geoName";

    public GeoName(GeoNameRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public GeoName(String name, String id) {
        super(TABLE_NAME, new GeoNameRowKey(name, id));
    }

    public GeoName(RowKey rowKey) {
        super(TABLE_NAME, new GeoNameRowKey(rowKey.toString()));
    }

    public GeoNameMetadata getMetadata() {
        GeoNameMetadata metadata = get(GeoNameMetadata.NAME);
        if (metadata == null) {
            addColumnFamily(new GeoNameMetadata());
        }
        return get(GeoNameMetadata.NAME);
    }
}
