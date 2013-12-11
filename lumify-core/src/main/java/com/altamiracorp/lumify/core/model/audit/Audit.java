package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import org.json.JSONException;
import org.json.JSONObject;

public class Audit extends Row<AuditRowKey> {
    public static final String TABLE_NAME = "atc_audit";

    public Audit(RowKey rowKey) {
        super(TABLE_NAME, new AuditRowKey(rowKey.toString()));
    }

    public Audit(String rowKey) {
        super(TABLE_NAME, new AuditRowKey(rowKey));
    }

    public Audit() {
        super(TABLE_NAME);
    }

    public AuditData getData() {
        AuditData auditData = get(AuditData.NAME);
        if (auditData == null) {
            addColumnFamily(new AuditData());
        }
        return get(AuditData.NAME);
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("message", this.getData().getMessage());
            json.put("user", this.getData().getUserRowKey());
            String[] rowKey = RowKeyHelper.splitOnMinorFieldSeperator(this.getRowKey().toString());
            json.put("graphVertexID", rowKey[0]);
            json.put("dateTime", rowKey[1]);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
