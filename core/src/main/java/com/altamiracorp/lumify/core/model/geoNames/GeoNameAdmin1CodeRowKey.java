package com.altamiracorp.lumify.core.model.geoNames;

import com.altamiracorp.bigtable.model.RowKey;

public class GeoNameAdmin1CodeRowKey extends RowKey {
    public GeoNameAdmin1CodeRowKey(String rowKey) {
        super(rowKey);
    }

    public GeoNameAdmin1CodeRowKey(String countryCode, String admin1Code) {
        this(countryCode + "." + admin1Code);
    }
}
