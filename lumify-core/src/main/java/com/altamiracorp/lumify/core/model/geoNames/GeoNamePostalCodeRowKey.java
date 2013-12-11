package com.altamiracorp.lumify.core.model.geoNames;

import com.altamiracorp.bigtable.model.RowKey;

public class GeoNamePostalCodeRowKey extends RowKey {

    public GeoNamePostalCodeRowKey (String countryCode, String zipCode) {
        this(countryCode + "." + zipCode);
    }

    public GeoNamePostalCodeRowKey(String rowKey) {
        super(rowKey);
    }

    public String getPostalCode () {
        return this.toString().split("\\.")[1];
    }

    public String getCountryCode () {
        return this.toString().split("\\.")[0];
    }
}
