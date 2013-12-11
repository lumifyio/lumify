package com.altamiracorp.lumify.core.model.geoNames;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;

public class GeoNameAdmin1Code extends Row<GeoNameAdmin1CodeRowKey> {
    public static final String TABLE_NAME = "atc_geoNameAdmin1Code";

    public GeoNameAdmin1Code(GeoNameAdmin1CodeRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public GeoNameAdmin1Code(RowKey rowKey) {
        super(TABLE_NAME, new GeoNameAdmin1CodeRowKey(rowKey.toString()));
    }

    public GeoNameAdmin1CodeMetadata getMetadata() {
        GeoNameAdmin1CodeMetadata metadata = get(GeoNameAdmin1CodeMetadata.NAME);
        if (metadata == null) {
            addColumnFamily(new GeoNameAdmin1CodeMetadata());
        }
        return get(GeoNameAdmin1CodeMetadata.NAME);
    }
}
