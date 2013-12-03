package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.BaseBuilder;
import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Row;

import java.util.Collection;

public class AuditBuilder extends BaseBuilder<Audit> {
    public Audit fromRow(Row row) {
        Audit audit = new Audit(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            if (columnFamily.getColumnFamilyName().equals(AuditData.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                audit.addColumnFamily(new AuditData().addColumns(columns));
            } else {
                audit.addColumnFamily(columnFamily);
            }
        }
        return audit;
    }

    @Override
    public String getTableName() {
        return Audit.TABLE_NAME;
    }
}
