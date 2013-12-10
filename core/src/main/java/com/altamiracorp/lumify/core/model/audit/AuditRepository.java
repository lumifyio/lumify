package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.user.User;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public void audit(String vertexId, ArrayList<String> messages, User user) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkArgument(vertexId.length() > 0, "vertexId cannot be empty");
        checkNotNull(messages, "message cannot be null");
        checkNotNull(user, "user cannot be null");

        if (messages.size() < 1) {
            return;
        }

        for (String message : messages) {
            audit(vertexId, message, user);
        }
    }

    public ArrayList<String> vertexPropertyAuditMessages(GraphVertex vertex, List<String> modifiedProperties) {
        ArrayList<String> messages = new ArrayList<String>();
        HashMap<String, Object> oldProperties = vertex.getOldProperties();
        for (String modifiedProperty : modifiedProperties) {
            Object oldProperty = "undefined";
            if (oldProperties.containsKey(modifiedProperty)) {
                if (oldProperties.equals(vertex.getProperty(modifiedProperty))) {
                    continue;
                } else {
                    oldProperty = oldProperties.get(modifiedProperty);
                }
            }
            messages.add("Set " + modifiedProperty + " from " + oldProperty + " to " + vertex.getProperty(modifiedProperty));
        }
        return messages;
    }

    public String relationshipAuditMessageOnSource (String label, Object destTitle) {
        return label + " relationship created to " + destTitle;
    }

    public String relationshipAuditMessageOnDest (String label, Object sourceTitle) {
        return label + " relationship created from " + sourceTitle;
    }

    public String relationshipAuditMessageOnArtifact (Object sourceTitle, Object destTitle, String label) {
        return label + " relationship created from " + sourceTitle + " to " + destTitle;
    }

    public String resolvedEntityAuditMessageForArtifact(Object entityTitle) {
        return "Resolved entity, " + entityTitle;
    }

    public String resolvedEntityAuditMessage(Object artifactTitle) {
        return "Resolved entity from " + artifactTitle;
    }

    public String deleteEntityAuditMessage(Object title) {
        return "Deleted entity, " + title;
    }
}
