package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.bigtable.model.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Collection;

@Singleton
public class WorkspaceRepository extends Repository<Workspace> {
    @Inject
    public WorkspaceRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public Workspace fromRow(Row row) {
        Workspace artifact = new Workspace(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            String columnFamilyName = columnFamily.getColumnFamilyName();
            if (columnFamilyName.equals(WorkspaceContent.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                artifact.addColumnFamily(new WorkspaceContent().addColumns(columns));
            } else if (columnFamilyName.equals(WorkspaceMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                artifact.addColumnFamily(new WorkspaceMetadata().addColumns(columns));
            } else {
                artifact.addColumnFamily(columnFamily);
            }
        }
        return artifact;
    }

    @Override
    public Row toRow(Workspace workspace) {
        return workspace;
    }

    @Override
    public String getTableName() {
        return Workspace.TABLE_NAME;
    }
}
