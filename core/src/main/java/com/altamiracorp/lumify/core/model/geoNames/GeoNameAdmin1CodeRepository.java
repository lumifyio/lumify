package com.altamiracorp.lumify.core.model.geoNames;

import java.util.Collection;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.google.inject.Inject;

public class GeoNameAdmin1CodeRepository extends Repository<GeoNameAdmin1Code> {
    @Inject
    public GeoNameAdmin1CodeRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public GeoNameAdmin1Code fromRow(Row row) {
        GeoNameAdmin1Code geoNameAdmin1Code = new GeoNameAdmin1Code(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            if (columnFamily.getColumnFamilyName().equals(GeoNameAdmin1CodeMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                geoNameAdmin1Code.addColumnFamily(new GeoNameAdmin1CodeMetadata().addColumns(columns));
            } else {
                geoNameAdmin1Code.addColumnFamily(columnFamily);
            }
        }
        return geoNameAdmin1Code;
    }

    @Override
    public Row toRow(GeoNameAdmin1Code geoName) {
        return geoName;
    }

    @Override
    public String getTableName() {
        return GeoNameAdmin1Code.TABLE_NAME;
    }

    public GeoNameAdmin1Code findByCountryAndAdmin1Code(String countryCode, String admin1Code, User user) {
        return findByRowKey(new GeoNameAdmin1CodeRowKey(countryCode, admin1Code).toString(), user.getModelUserContext());
    }
}
