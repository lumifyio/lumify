package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.util.RowKeyHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class AuditRowKey extends RowKey {
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public AuditRowKey(String rowKey) {
        super(rowKey);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static AuditRowKey build(Object vertexId) {
        Date date = new Date();
        return new AuditRowKey(RowKeyHelper.buildMinor(vertexId.toString(), dateFormat.format(date)));
    }

    public static AuditRowKey build (Object sourceId, Object destId) {
        Date date = new Date();
        String prefix = sourceId + ":" + destId;
        return new AuditRowKey(RowKeyHelper.buildMinor(prefix, dateFormat.format(date)));
    }
}
