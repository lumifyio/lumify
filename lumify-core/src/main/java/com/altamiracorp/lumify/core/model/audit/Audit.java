package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
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

    public AuditCommon getAuditCommon() {
        AuditCommon auditCommon = get(AuditCommon.NAME);
        if (auditCommon == null) {
            addColumnFamily(new AuditCommon());
        }
        return get(AuditCommon.NAME);
    }

    public AuditEntity getAuditEntity() {
        AuditEntity auditEntity = get(AuditEntity.NAME);
        if (auditEntity == null) {
            addColumnFamily(new AuditEntity());
        }
        return get(AuditEntity.NAME);
    }

    public AuditRelationship getAuditRelationship() {
        AuditRelationship auditRelationship = get(AuditRelationship.NAME);
        if (auditRelationship == null) {
            addColumnFamily(new AuditRelationship());
        }
        return get(AuditRelationship.NAME);
    }

    public AuditProperty getAuditProperty() {
        AuditProperty auditProperty = get(AuditProperty.NAME);
        if (auditProperty == null) {
            addColumnFamily(new AuditProperty());
        }
        return get(AuditProperty.NAME);
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("data", this.getAuditCommon().toJson());
            if (this.getAuditCommon().getType().equals(OntologyRepository.TYPE_PROPERTY.toString())) {
                json.put("propertyAudit", this.getAuditProperty().toJson());
            } else if (this.getAuditCommon().getType().equals(OntologyRepository.TYPE_RELATIONSHIP.toString())) {
                json.put("relationshipAudit", this.getAuditRelationship().toJson());
            } else {
                json.put("entityAudit", this.getAuditEntity().toJson());
            }
            String[] rowKey = RowKeyHelper.splitOnMinorFieldSeperator(getRowKey().toString());
            json.put("graphVertexID", rowKey[0]);
            json.put("dateTime", rowKey[1]);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
