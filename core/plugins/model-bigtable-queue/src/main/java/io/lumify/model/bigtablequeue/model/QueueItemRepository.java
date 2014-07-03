package io.lumify.model.bigtablequeue.model;

import com.altamiracorp.bigtable.model.*;

import java.util.Collection;

public class QueueItemRepository extends Repository<QueueItem> {
    private final String tableName;

    public QueueItemRepository(ModelSession modelSession, String tableName) {
        super(modelSession);
        this.tableName = tableName;
    }

    @Override
    public QueueItem fromRow(Row row) {
        QueueItem queueItem = new QueueItem(this.tableName, new QueueItemRowKey(row.getRowKey()));
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            queueItem.addColumnFamily(columnFamily);
        }
        return queueItem;
    }

    @Override
    public Row toRow(QueueItem row) {
        return row;
    }

    @Override
    public String getTableName() {
        return this.tableName;
    }
}
