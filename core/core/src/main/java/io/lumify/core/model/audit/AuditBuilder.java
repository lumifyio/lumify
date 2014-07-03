package io.lumify.core.model.audit;

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
            if (columnFamily.getColumnFamilyName().equals(AuditCommon.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                audit.addColumnFamily(new AuditCommon().addColumns(columns));
            } else if (columnFamily.getColumnFamilyName().equals(AuditProperty.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                audit.addColumnFamily(new AuditProperty().addColumns(columns));
            } else if (columnFamily.getColumnFamilyName().equals(AuditRelationship.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                audit.addColumnFamily(new AuditRelationship().addColumns(columns));
            } else if (columnFamily.getColumnFamilyName().equals(AuditEntity.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                audit.addColumnFamily(new AuditEntity().addColumns(columns));
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
