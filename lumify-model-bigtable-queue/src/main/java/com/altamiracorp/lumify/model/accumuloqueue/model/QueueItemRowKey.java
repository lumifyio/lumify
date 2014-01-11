package com.altamiracorp.lumify.model.accumuloqueue.model;

import com.altamiracorp.bigtable.model.RowKey;

public class QueueItemRowKey extends RowKey {
    public QueueItemRowKey(long time) {
        super(Long.toHexString(time));
    }

    public QueueItemRowKey(RowKey rowKey) {
        this(Long.parseLong(rowKey.toString(), 16));
    }

    public QueueItemRowKey(Object msgId) {
        this(Long.parseLong(msgId.toString(), 16));
    }
}
