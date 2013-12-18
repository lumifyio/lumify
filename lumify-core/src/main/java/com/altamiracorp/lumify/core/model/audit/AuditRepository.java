package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.VertexType;
import com.altamiracorp.lumify.core.user.User;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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

    public List<Audit> auditEntity(String action, String entityId, String artifactId, String process, String comment, User user) {
        checkNotNull(action, "action cannot be null");
        checkArgument(action.length() > 0, "action cannot be empty");
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
                .setAction(action)
                .setType(VertexType.ENTITY.toString())
                .setComment(comment)
                .setProcess(process);

        auditArtifact.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(VertexType.ENTITY.toString())
                .setComment(comment)
                .setProcess(process);

        List<Audit> audits = Lists.newArrayList(auditEntity, auditArtifact);
        saveMany(audits, user.getModelUserContext());
        return audits;
    }

    public Audit auditProperties(String action, GraphVertex entity, String propertyName, String process, String comment, User user) {
        checkNotNull(action, "action cannot be null");
        checkArgument(action.length() > 0, "action cannot be empty");
        checkNotNull(entity, "entity cannot be null");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkArgument(propertyName.length() > 0, "property name cannot be empty");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit audit = new Audit(AuditRowKey.build(entity.getId()));
        HashMap<String, Object> oldProperties = entity.getOldProperties();

        audit.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(VertexType.PROPERTY.toString())
                .setComment(comment)
                .setProcess(process);

        if (oldProperties.containsKey(propertyName)) {
            audit.getAuditProperty().setPreviousValue(oldProperties.get(propertyName).toString());
        }
        audit.getAuditProperty().setNewValue(entity.getProperty(propertyName).toString());
        audit.getAuditProperty().setPropertyName(propertyName);

        save(audit, user.getModelUserContext());
        return audit;
    }

    public List<Audit> auditRelationships (String action, GraphVertex sourceVertex, GraphVertex destVertex, String label, String process, String comment, User user) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(action.length() > 0, "action cannot be empty");
        checkNotNull(sourceVertex, "sourceVertex cannot be null");
        checkNotNull(destVertex, "destVertex cannot be null");
        checkNotNull(label, "label cannot be null");
        checkArgument(label.length() > 0, "label cannot be empty");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit auditSourceDest = new Audit(AuditRowKey.build(sourceVertex.getId(), destVertex.getId()));
        Audit auditDestSource = new Audit(AuditRowKey.build(destVertex.getId(), sourceVertex.getId()));

        auditSourceDest.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(VertexType.RELATIONSHIP.toString())
                .setComment(comment)
                .setProcess(process);

        auditDestSource.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(VertexType.RELATIONSHIP.toString())
                .setComment(comment)
                .setProcess(process);

        auditSourceDest.getAuditRelationship()
                .setSourceId(sourceVertex.getId())
                .setSourceType(sourceVertex.getProperty(PropertyName.SUBTYPE.toString()))
                .setSourceTitle(sourceVertex.getProperty(PropertyName.TITLE.toString()))
                .setDestId(destVertex.getId())
                .setDestTitle(destVertex.getProperty(PropertyName.TITLE.toString()))
                .setDestType(destVertex.getProperty(PropertyName.TYPE.toString()))
                .setLabel(label);

        auditDestSource.getAuditRelationship()
                .setSourceId(sourceVertex.getId())
                .setSourceType(sourceVertex.getProperty(PropertyName.SUBTYPE.toString()))
                .setSourceTitle(sourceVertex.getProperty(PropertyName.TITLE.toString()))
                .setDestId(destVertex.getId())
                .setDestTitle(destVertex.getProperty(PropertyName.TITLE.toString()))
                .setDestType(destVertex.getProperty(PropertyName.TYPE.toString()))
                .setLabel(label);

        List<Audit> audits = Lists.newArrayList(auditDestSource, auditSourceDest);
        saveMany(audits, user.getModelUserContext());
        return audits;
    }
}
