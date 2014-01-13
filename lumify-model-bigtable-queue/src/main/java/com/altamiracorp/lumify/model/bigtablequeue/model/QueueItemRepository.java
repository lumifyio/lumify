package com.altamiracorp.lumify.model.bigtablequeue.model;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.user.User;
import org.json.JSONObject;

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

    public void add(JSONObject json, String[] extra, User user) {
        QueueItem queueItem = new QueueItem(this.tableName, json, extra);
        this.save(queueItem, user.getModelUserContext());
    }
}
