package com.altamiracorp.lumify.web.session.model;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.google.inject.Inject;

public class JettySessionRepository extends Repository<JettySessionRow> {

    @Inject
    public JettySessionRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public JettySessionRow fromRow(Row row) {
        JettySessionRow jettySession = new JettySessionRow(row.getRowKey());
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
