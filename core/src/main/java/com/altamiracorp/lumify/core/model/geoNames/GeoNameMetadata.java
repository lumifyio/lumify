package com.altamiracorp.lumify.core.model.geoNames;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

public class GeoNameMetadata extends ColumnFamily {
    public static final String NAME = "Metadata";
    public static final String NAME_COLUMN = "name";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String POPULATION = "population";
    public static final String COUNTRY_CODE = "countryCode";
    public static final String ALTERNATE_COUNTRY_CODE = "alternateCountryCode";
    public static final String ADMIN_1_CODE = "admin1Code";
    public static final String ADMIN_2_CODE = "admin2Code";
    public static final String ADMIN_3_CODE = "admin3Code";
    public static final String ADMIN_4_CODE = "admin4Code";
    public static final String FEATURE_CODE = "featureCode";
    public static final String FEATURE_CLASS = "featureClass";

    public GeoNameMetadata() {
        super(NAME);
    }

    public String getName() {
        return Value.toString(get(NAME_COLUMN));
    }

    public GeoNameMetadata setName(String name) {
        set(NAME_COLUMN, name);
        return this;
    }

    public Double getLatitude() {
        return Value.toDouble(get(LATITUDE));
    }

    public GeoNameMetadata setLatitude(double latitude) {
        set(LATITUDE, latitude);
        return this;
    }

    public Double getLongitude() {
        return Value.toDouble(get(LONGITUDE));
    }

    public GeoNameMetadata setLongitude(Double longitude) {
        set(LONGITUDE, longitude);
        return this;
    }

    public Long getPopulation() {
        return Value.toLong(get(POPULATION));
    }

    public GeoNameMetadata setPopulation(Long population) {
        set(POPULATION, population);
        return this;
    }

    public String getCountryCode() {
        return Value.toString(get(COUNTRY_CODE));
    }

    public GeoNameMetadata setCountryCode(String countryCode) {
        set(COUNTRY_CODE, countryCode);
        return this;
    }

    public String getAlternateCountryCodes() {
        return Value.toString(get(ALTERNATE_COUNTRY_CODE));
    }

    public GeoNameMetadata setAlternateCountryCodes(String alternateCountryCode) {
        set(ALTERNATE_COUNTRY_CODE, alternateCountryCode);
        return this;
    }

    public String getAdmin1Code() {
        return Value.toString(get(ADMIN_1_CODE));
    }

    public GeoNameMetadata setAdmin1Code(String admin1Code) {
        set(ADMIN_1_CODE, admin1Code);
        return this;
    }

    public String getAdmin2Code() {
        return Value.toString(get(ADMIN_2_CODE));
    }

    public GeoNameMetadata setAdmin2Code(String admin2Code) {
        set(ADMIN_2_CODE, admin2Code);
        return this;
    }

    public String getAdmin3Code() {
        return Value.toString(get(ADMIN_3_CODE));
    }

    public GeoNameMetadata setAdmin3Code(String admin3Code) {
        set(ADMIN_3_CODE, admin3Code);
        return this;
    }

    public String getAdmin4Code() {
        return Value.toString(get(ADMIN_4_CODE));
    }

    public GeoNameMetadata setAdmin4Code(String admin4Code) {
        set(ADMIN_4_CODE, admin4Code);
        return this;
    }

    public String getFeatureCode() {
        return Value.toString(get(FEATURE_CODE));
    }

    public GeoNameMetadata setFeatureCode(String featureCode) {
        set(FEATURE_CODE, featureCode);
        return this;
    }

    public String getFeatureClass() {
        return Value.toString(get(FEATURE_CLASS));
    }

    public GeoNameMetadata setFeatureClass(String featureClass) {
        set(FEATURE_CLASS, featureClass);
        return this;
    }
}
