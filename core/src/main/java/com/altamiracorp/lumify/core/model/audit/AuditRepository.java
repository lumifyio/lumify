package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class AuditRepository extends Repository<Audit> {
    private final AuditBuilder auditBuilder = new AuditBuilder();

    @Inject
    public AuditRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public Audit fromRow(Row row) {
        return auditBuilder.fromRow(row);
    }

    @Override
    public Row toRow(Audit audit) {
        return audit;
    }

    @Override
    public String getTableName() {
        return auditBuilder.getTableName();
    }

    public Audit audit(String vertexId, String message, User user) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkArgument(vertexId.length() > 0, "vertexId cannot be empty");
        checkNotNull(message, "message cannot be null");
        checkArgument(message.length() > 0, "message cannot be empty");
        checkNotNull(user, "user cannot be null");

        Audit audit = new Audit(AuditRowKey.build(vertexId));
        audit.getData()
                .setMessage(message)
                .setUser(user);
        save(audit, user.getModelUserContext());
        return audit;
    }

    public String vertexPropertyAuditMessage (String propertyName, Object value) {
        return "Set " + propertyName + " from undefined to " + value;
    }

    public String vertexPropertyAuditMessage(GraphVertex graphVertex, String propertyName, Object newValue) {
        Object oldValue;
        if (graphVertex.getProperty(propertyName) == null) {
            oldValue = "undefined";
        } else {
            oldValue = graphVertex.getProperty(propertyName);
        }
        return "Set " + propertyName + " from " + oldValue + " to " + newValue;
    }

    public String createEntityAuditMessage () {
        return "Entity Created";
    }

    public String updateEntityAuditMessage () {
        return "Updating Entity";
    }

    public String deleteEntityAuditMessage (String deletedVertexId) {
        return "Deleted: " + deletedVertexId;
    }
}
