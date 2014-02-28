package com.altamiracorp.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.BaseBuilder;
import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Row;

import java.util.Collection;

public class DetectedObjectBuilder extends BaseBuilder<DetectedObjectModel> {
    @Override
    public DetectedObjectModel fromRow(Row row) {
        DetectedObjectModel detectedObject = new DetectedObjectModel(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            if (columnFamily.getColumnFamilyName().equals(DetectedObjectMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                detectedObject.addColumnFamily(new DetectedObjectMetadata().addColumns(columns));
            } else {
                detectedObject.addColumnFamily(columnFamily);
            }
        }
        return detectedObject;
    }

    @Override
    public String getTableName() {
        return DetectedObjectModel.TABLE_NAME;
    }
}