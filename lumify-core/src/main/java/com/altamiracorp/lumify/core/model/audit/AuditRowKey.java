package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.util.RowKeyHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AuditRowKey extends RowKey {
    public AuditRowKey(String rowKey) {
        super(rowKey);
    }

    public static AuditRowKey build(String vertexId) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date();
        return new AuditRowKey(RowKeyHelper.buildMinor(vertexId, dateFormat.format(date)));
    }
}
