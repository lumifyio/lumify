package com.altamiracorp.lumify.core.model.geoNames;

import com.altamiracorp.bigtable.model.*;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GeoNameRepository extends Repository<GeoName> {
    @Inject
    public GeoNameRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public GeoName fromRow(Row row) {
        GeoName geoName = new GeoName(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            if (columnFamily.getColumnFamilyName().equals(GeoNameMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                geoName.addColumnFamily(new GeoNameMetadata().addColumns(columns));
            } else {
                geoName.addColumnFamily(columnFamily);
            }
        }
        return geoName;
    }

    @Override
    public Row toRow(GeoName geoName) {
        return geoName;
    }

    @Override
    public String getTableName() {
        return GeoName.TABLE_NAME;
    }

    public GeoName findBestMatch(String name, User user) {
        Iterable<GeoName> matches = findByRowStartsWith(name.toLowerCase() + RowKeyHelper.MINOR_FIELD_SEPARATOR, user.getModelUserContext());
        List<GeoName> geoNames = new ArrayList<GeoName>();
        for (GeoName g : matches) {
            geoNames.add(g);
        }
        if (geoNames.size() == 0) {
            return null;
        }

        Collections.sort(geoNames, new GeoNamePopulationComparator());
        return geoNames.get(geoNames.size() - 1);
    }
}
