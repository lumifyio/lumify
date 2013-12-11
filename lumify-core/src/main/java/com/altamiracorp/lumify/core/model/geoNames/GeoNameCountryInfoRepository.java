package com.altamiracorp.lumify.core.model.geoNames;

import java.util.Collection;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.google.inject.Inject;

public class GeoNameCountryInfoRepository extends Repository<GeoNameCountryInfo> {
    @Inject
    public GeoNameCountryInfoRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public GeoNameCountryInfo fromRow(Row row) {
        GeoNameCountryInfo geoNameCountryInfo = new GeoNameCountryInfo(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            if (columnFamily.getColumnFamilyName().equals(GeoNameAdmin1CodeMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                geoNameCountryInfo.addColumnFamily(new GeoNameCountryInfoMetadata().addColumns(columns));
            } else {
                geoNameCountryInfo.addColumnFamily(columnFamily);
            }
        }
        return geoNameCountryInfo;
    }

    @Override
    public Row toRow(GeoNameCountryInfo geoName) {
        return geoName;
    }

    @Override
    public String getTableName() {
        return GeoNameCountryInfo.TABLE_NAME;
    }

    public GeoNameCountryInfo findByCountryCode(String countryCode, User user) {
        return findByRowKey(new GeoNameCountryInfoRowKey(countryCode).toString(), user.getModelUserContext());
    }
}
