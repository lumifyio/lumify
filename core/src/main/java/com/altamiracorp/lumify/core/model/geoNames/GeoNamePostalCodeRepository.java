package com.altamiracorp.lumify.core.model.geoNames;

import java.util.Collection;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.google.inject.Inject;

public class GeoNamePostalCodeRepository extends Repository<GeoNamePostalCode> {
    @Inject
    public GeoNamePostalCodeRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public GeoNamePostalCode fromRow(Row row) {
        GeoNamePostalCode geoNamePostalCode = new GeoNamePostalCode(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            if (columnFamily.getColumnFamilyName().equals(GeoNamePostalCodeMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                geoNamePostalCode.addColumnFamily(new GeoNamePostalCodeMetadata().addColumns(columns));
            } else {
                geoNamePostalCode.addColumnFamily(columnFamily);
            }
        }
        return geoNamePostalCode;
    }

    @Override
    public Row toRow(GeoNamePostalCode geoNamePostalCode) {
        return geoNamePostalCode;
    }

    @Override
    public String getTableName() {
        return GeoNamePostalCode.TABLE_NAME;
    }

    public GeoNamePostalCode findByCountryAndPostalCode(String countryCode, String postalCode, User user) {
        return this.findByRowKey(new GeoNamePostalCodeRowKey(countryCode, postalCode).toString(), user.getModelUserContext());
    }

    public GeoNamePostalCode findByUSZipCode(String zipCode, User user) {
        return findByCountryAndPostalCode("US", zipCode, user);
    }
}
