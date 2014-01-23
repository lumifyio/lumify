package com.altamiracorp.lumify.web.session.model;

import com.altamiracorp.bigtable.model.*;
import com.google.inject.Inject;

import java.util.Collection;

public class JettySessionRepository extends Repository<JettySessionRow> {

    @Inject
    public JettySessionRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public JettySessionRow fromRow(Row row) {
        JettySessionRow jettySession = new JettySessionRow(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            if (columnFamily.getColumnFamilyName().equals(JettySessionMetadata.COLUMN_FAMILY_NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                jettySession.addColumnFamily(new JettySessionMetadata().addColumns(columns));
            } else if (columnFamily.getColumnFamilyName().equals(JettySessionData.COLUMN_FAMILY_NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                jettySession.addColumnFamily(new JettySessionData().addColumns(columns));
            } else {
                jettySession.addColumnFamily(columnFamily);
            }
        }
        return jettySession;
    }

    @Override
    public Row toRow(JettySessionRow row) {
        return row;
    }

    @Override
    public String getTableName() {
        return JettySessionRow.TABLE_NAME;
    }
}
