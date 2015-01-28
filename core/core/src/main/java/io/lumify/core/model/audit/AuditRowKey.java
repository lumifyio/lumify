package io.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.RowKey;
import io.lumify.core.util.RowKeyHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class AuditRowKey extends RowKey {
    public AuditRowKey(String rowKey) {
        super(rowKey);
    }

    public static AuditRowKey build(Object vertexId) {
        Date date = new Date();
        return new AuditRowKey(RowKeyHelper.buildMinor(vertexId.toString(), getDateFormat().format(date)));
    }

    public static AuditRowKey build(Object sourceId, Object destId) {
        Date date = new Date();
        String prefix = sourceId + ":" + destId;
        return new AuditRowKey(RowKeyHelper.buildMinor(prefix, getDateFormat().format(date)));
    }

    public static SimpleDateFormat getDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }
}
