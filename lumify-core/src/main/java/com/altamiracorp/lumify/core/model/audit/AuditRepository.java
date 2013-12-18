package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.VertexType;
import com.altamiracorp.lumify.core.user.User;
import com.google.common.collect.Lists;
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

    public void audit(String vertexId, ArrayList<String> messages, User user) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkArgument(vertexId.length() > 0, "vertexId cannot be empty");
        checkNotNull(messages, "message cannot be null");
        checkNotNull(user, "user cannot be null");

        if (messages.size() < 1) {
            return;
        }
    }

    public Audit audit(String vertexId, String message, User user) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkArgument(vertexId.length() > 0, "vertexId cannot be empty");
        checkNotNull(message, "message cannot be null");
        checkArgument(message.length() > 0, "message cannot be empty");
        checkNotNull(user, "user cannot be null");

        Audit audit = new Audit(AuditRowKey.build(vertexId));
        return audit;
    }

    public Audit auditVertexCreate(String vertexId, String process, String comment, User user) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkArgument(vertexId.length() > 0, "vertexId cannot be empty");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");
        checkNotNull(process, "process cannot be null");

        Audit audit = new Audit(AuditRowKey.build(vertexId));
        audit.getAuditCommon()
                .setUser(user)
                .setAction(AuditAction.CREATE.toString())
                .setType(VertexType.ENTITY.toString())
                .setComment(comment);

        if (process.length() > 0) {
            audit.getAuditCommon().setProcess(process);
        }

        save(audit, user.getModelUserContext());
        return audit;
    }

    public List<Audit> auditEntityResolution(String entityId, String artifactId, String process, String comment, User user) {
        checkNotNull(entityId, "entityId cannot be null");
        checkArgument(entityId.length() > 0, "entityId cannot be empty");
        checkNotNull(artifactId, "artifactId cannot be null");
        checkArgument(artifactId.length() > 0, "artifactId cannot be empty");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");
        checkNotNull(process, "process cannot be null");

        Audit auditArtifact = new Audit(AuditRowKey.build(artifactId));
        Audit auditEntity = new Audit(AuditRowKey.build(entityId));

        auditEntity.getAuditCommon()
                .setUser(user)
                .setAction(AuditAction.CREATE.toString())
                .setType(VertexType.ENTITY.toString())
                .setComment(comment)
                .setProcess(process);

        auditArtifact.getAuditCommon()
                .setUser(user)
                .setAction(AuditAction.CREATE.toString())
                .setType(VertexType.ENTITY.toString())
                .setComment(comment)
                .setProcess(process);

        List<Audit> audits = Lists.newArrayList(auditEntity, auditArtifact);
        saveMany(audits, user.getModelUserContext());
        return audits;
    }

    public Audit auditProperties(GraphVertex entity, String propertyName, String process, String comment, User user) {
        checkNotNull(entity, "entity cannot be null");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkArgument(propertyName.length() > 0, "property name cannot be empty");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");

        Audit audit = new Audit(AuditRowKey.build(entity.getId()));
        HashMap<String, Object> oldProperties = entity.getOldProperties();

        audit.getAuditCommon()
                .setUser(user)
                .setAction(AuditAction.UPDATE.toString())
                .setType(VertexType.PROPERTY.toString())
                .setComment(comment)
                .setProcess(process);

        if (oldProperties.containsKey(propertyName)) {
            audit.getAuditProperty().setPreviousValue(oldProperties.get(propertyName));
        }
        audit.getAuditProperty().setNewValue(entity.getProperty(propertyName));

        save(audit, user.getModelUserContext());
        return audit;
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

    public String relationshipAuditMessageOnSource(String label, Object destTitle, String titleOfCreationLocation) {
        String message = label + " relationship created to " + destTitle;
        if (titleOfCreationLocation != null && titleOfCreationLocation != "") {
            message = "In " + titleOfCreationLocation + ", " + message;
        }
        return message;
    }

    public String relationshipAuditMessageOnDest(String label, Object sourceTitle, String titleOfCreationLocation) {
        String message = label + " relationship created from " + sourceTitle;
        if (titleOfCreationLocation != "") {
            message = "In " + titleOfCreationLocation + ", " + message;
        }
        return message;
    }

    public String relationshipAuditMessageOnArtifact(Object sourceTitle, Object destTitle, String label) {
        return label + " relationship created from " + sourceTitle + " to " + destTitle;
    }

    public String deleteEntityAuditMessage(Object title) {
        return "Deleted entity, " + title;
    }
}
