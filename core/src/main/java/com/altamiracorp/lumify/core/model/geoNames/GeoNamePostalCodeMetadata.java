package com.altamiracorp.lumify.core.model.geoNames;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

public class GeoNamePostalCodeMetadata extends ColumnFamily{

    public static final String NAME = "Metadata";
    private static final String PLACE_NAME_COLUMN = "place_name";
    private static final String ADMIN1_CODE_COLUMN = "admin1";
    private static final String LATITUDE_COLUMN = "latitude";
    private static final String LONGITUDE_COLUMN = "longitude";

    public GeoNamePostalCodeMetadata() {
        super(NAME);
    }

    public String getPlaceName() {
        return Value.toString(get(PLACE_NAME_COLUMN));
    }

    public GeoNamePostalCodeMetadata setPlaceName(String placeName) {
        set(PLACE_NAME_COLUMN, placeName);
        return this;
    }

    public String getAdmin1Code() {
        return Value.toString(get(ADMIN1_CODE_COLUMN));
    }

    public GeoNamePostalCodeMetadata setAdmin1Code(String admin1Code) {
        set(ADMIN1_CODE_COLUMN, admin1Code);
        return this;
    }

    public Double getLatitude() {
        return Value.toDouble(get(LATITUDE_COLUMN));
    }

    public GeoNamePostalCodeMetadata setLatitude(Double latitude) {
        set(LATITUDE_COLUMN, latitude);
        return this;
    }

    public Double getLongitude() {
        return Value.toDouble(get(LONGITUDE_COLUMN));
    }

    public GeoNamePostalCodeMetadata setLongitude(Double longitude) {
        set(LONGITUDE_COLUMN, longitude);
        return this;
    }
}
