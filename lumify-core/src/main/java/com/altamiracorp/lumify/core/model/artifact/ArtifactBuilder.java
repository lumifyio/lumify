package com.altamiracorp.lumify.core.model.artifact;

import com.altamiracorp.bigtable.model.BaseBuilder;
import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Row;

import java.util.Collection;

public class ArtifactBuilder extends BaseBuilder<Artifact> {
    public Artifact fromRow(Row row) {
        Artifact artifact = new Artifact(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            if (columnFamily.getColumnFamilyName().equals(ArtifactMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                artifact.addColumnFamily(new ArtifactMetadata().addColumns(columns));
            } else {
                artifact.addColumnFamily(columnFamily);
            }
        }
        return artifact;
    }

    @Override
    public String getTableName() {
        return Artifact.TABLE_NAME;
    }
}
