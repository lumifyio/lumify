package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.model.PropertyJustificationMetadata;
import com.altamiracorp.lumify.core.model.PropertySourceMetadata;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.version.VersionService;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.type.GeoPoint;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class AuditRepository extends Repository<Audit> {
    private final AuditBuilder auditBuilder = new AuditBuilder();
    private final VersionService versionService;

    @Inject
    public AuditRepository(final ModelSession modelSession, final VersionService versionService) {
        super(modelSession);
        this.versionService = versionService;
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

    public Audit auditVertexCreate(Object vertexId, String process, String comment, User user) {
        return auditVertex(AuditAction.CREATE, vertexId, process, comment, user, FlushFlag.DEFAULT);
    }

    public Audit auditVertex(AuditAction auditAction, Object vertexId, String process, String comment, User user, FlushFlag flushFlag) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");
        checkNotNull(process, "process cannot be null");

        Audit audit = new Audit(AuditRowKey.build(vertexId));
        audit.getAuditCommon()
                .setUser(user)
                .setAction(auditAction)
                .setType(OntologyRepository.TYPE_ENTITY)
                .setComment(comment)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        if (process.length() > 0) {
            audit.getAuditCommon().setProcess(process);
        }

        save(audit, flushFlag);
        return audit;
    }

    public List<Audit> auditEntity(
            AuditAction action,
            Object entityId,
            String artifactId,
            String entityTitle,
            String entitySubtype,
            String process,
            String comment,
            User user) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(entityId, "entityId cannot be null");
        checkArgument(entityId.toString().length() > 0, "entityId cannot be empty");
        checkNotNull(artifactId, "artifactId cannot be null");
        checkArgument(artifactId.length() > 0, "artifactId cannot be empty");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");
        checkNotNull(process, "process cannot be null");
        checkNotNull(entityTitle, "entity title cannot be null");
        checkArgument(entityTitle.length() > 0, "entity title cannot be empty");
        checkNotNull(entitySubtype, "entity subtype cannot be null");
        checkArgument(entitySubtype.length() > 0, "entity subtype cannot be empty");

        Audit auditArtifact = new Audit(AuditRowKey.build(artifactId));
        Audit auditEntity = new Audit(AuditRowKey.build(entityId));

        List<Audit> audits = new ArrayList<Audit>();
        audits.add(auditEntityHelper(auditEntity, action, entityId, entityTitle, entitySubtype, process, comment, user));
        audits.add(auditEntityHelper(auditArtifact, action, entityId, entityTitle, entitySubtype, process, comment, user));
        saveMany(audits);
        return audits;
    }

    public Audit auditEntityProperty(AuditAction action, Object id, String propertyName, Object oldValue, Object newValue,
                                     String process, String comment, Map<String, Object> metadata, User user) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(id, "id cannot be null");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkArgument(propertyName.length() > 0, "property name cannot be empty");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit audit = new Audit(AuditRowKey.build(id));

        audit.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_PROPERTY)
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        if (oldValue != null) {
            if (oldValue instanceof GeoPoint) {
                String val = String.format("POINT(%f,%f)", ((GeoPoint) oldValue).getLatitude(), ((GeoPoint) oldValue).getLongitude());
                audit.getAuditProperty().setPreviousValue(val);
            } else {
                audit.getAuditProperty().setPreviousValue(oldValue.toString());
            }
        }
        if (action == AuditAction.DELETE) {
            audit.getAuditProperty().setNewValue("");
        } else {
            if (newValue instanceof GeoPoint) {
                String val = String.format("POINT(%f,%f)", ((GeoPoint) newValue).getLatitude(), ((GeoPoint) newValue).getLongitude());
                audit.getAuditProperty().setNewValue(val);
            } else {
                audit.getAuditProperty().setNewValue(newValue.toString());
            }
        }
        audit.getAuditProperty().setPropertyName(propertyName);

        if (metadata != null || !metadata.isEmpty()) {
            audit.getAuditProperty().setPropertyMetadata(jsonMetadata(metadata).toString());
        }

        save(audit);
        return audit;
    }

    public List<Audit> auditRelationship(AuditAction action, Vertex sourceVertex, Vertex destVertex, String label, String process, String comment, User user) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(sourceVertex, "sourceVertex cannot be null");
        checkNotNull(destVertex, "destVertex cannot be null");
        checkNotNull(label, "label cannot be null");
        checkArgument(label.length() > 0, "label cannot be empty");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit auditSourceDest = new Audit(AuditRowKey.build(sourceVertex.getId(), destVertex.getId()));
        Audit auditDestSource = new Audit(AuditRowKey.build(destVertex.getId(), sourceVertex.getId()));

        List<Audit> audits = new ArrayList<Audit>();
        audits.add(auditRelationshipHelper(auditSourceDest, action, sourceVertex, destVertex, label, process, comment, user));
        audits.add(auditRelationshipHelper(auditDestSource, action, sourceVertex, destVertex, label, process, comment, user));
        saveMany(audits);
        return audits;
    }

    public List<Audit> auditRelationshipProperty(AuditAction action, String sourceId, String destId, String propertyName,
                                                 Object oldValue, Edge edge, String process, String comment, User user) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(sourceId, "sourceId cannot be null");
        checkNotNull(sourceId.length() > 0, "sourceId cannot be empty");
        checkNotNull(destId, "destId cannot be null");
        checkNotNull(destId.length() > 0, "destId cannot be empty");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkNotNull(propertyName.length() > 0, "propertyName cannot be empty");
        checkNotNull(oldValue, "oldValue cannot be null");
        checkNotNull(edge, "edge cannot be null");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit auditSourceDest = new Audit(AuditRowKey.build(sourceId, destId));
        Audit auditDestSource = new Audit(AuditRowKey.build(destId, sourceId));

        auditSourceDest.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_PROPERTY)
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        auditDestSource.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_PROPERTY)
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        if (!oldValue.equals("")) {
            auditDestSource.getAuditProperty().setPreviousValue(oldValue);
            auditSourceDest.getAuditProperty().setPreviousValue(oldValue);
        }
        if (action == AuditAction.DELETE) {
            auditDestSource.getAuditProperty().setNewValue("");
            auditSourceDest.getAuditProperty().setNewValue("");
        } else {
            // TODO handle multi-valued properties
            auditDestSource.getAuditProperty().setNewValue(edge.getPropertyValue(propertyName, 0));
            auditSourceDest.getAuditProperty().setNewValue(edge.getPropertyValue(propertyName, 0));
        }
        auditDestSource.getAuditProperty().setPropertyName(propertyName);
        auditSourceDest.getAuditProperty().setPropertyName(propertyName);

        List<Audit> audits = Lists.newArrayList(auditSourceDest, auditDestSource);
        saveMany(audits);
        return audits;
    }

    private Audit auditEntityHelper(Audit audit, AuditAction action, Object entityID, String entityTitle, String entitySubtype, String process, String comment, User user) {
        audit.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_ENTITY)
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        audit.getAuditEntity()
                .setTitle(entityTitle)
                .setType(OntologyRepository.TYPE_ENTITY)
                .setSubtype(entitySubtype)
                .setID(entityID.toString());
        return audit;
    }

    private Audit auditRelationshipHelper(Audit audit, AuditAction action, Vertex sourceVertex, Vertex destVertex, String label, String process, String comment, User user) {
        audit.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_RELATIONSHIP)
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        audit.getAuditRelationship()
                .setSourceId(sourceVertex.getId())
                .setSourceType(CONCEPT_TYPE.getPropertyValue(sourceVertex))
                .setSourceTitle(TITLE.getPropertyValue(sourceVertex))
                .setDestId(destVertex.getId())
                .setDestTitle(TITLE.getPropertyValue(destVertex))
                .setDestType(CONCEPT_TYPE.getPropertyValue(destVertex))
                .setLabel(label);
        return audit;
    }

    public void auditVertexElementMutation(ElementMutation<Vertex> vertexElementMutation, Vertex vertex, String process, User user) {
        if (vertexElementMutation instanceof ExistingElementMutation) {
            Vertex oldVertex = (Vertex) ((ExistingElementMutation) vertexElementMutation).getElement();
            for (Property property : vertexElementMutation.getProperties()) {
                // TODO handle multi-valued properties
                Object oldPropertyValue = oldVertex.getPropertyValue(property.getName());
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property value cannot be null");
                if (!newPropertyValue.equals(oldPropertyValue)) {
                    auditEntityProperty(AuditAction.UPDATE, oldVertex.getId(), property.getName(), oldPropertyValue,
                            newPropertyValue, process, "", property.getMetadata(), user);
                }
            }
        } else {
            auditVertexCreate(vertex.getId(), process, "", user);
            for (Property property : vertexElementMutation.getProperties()) {
                // TODO handle multi-valued properties
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property value cannot be null");
                auditEntityProperty(AuditAction.UPDATE, vertex.getId(), property.getName(), null, newPropertyValue, process, "",
                        property.getMetadata(), user);
            }
        }
    }

    private JSONObject jsonMetadata(Map<String, Object> metadata) {
        JSONObject json = new JSONObject();
        for (String key : metadata.keySet()) {
            if (key.equals(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION)) {
                json.put(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION, ((PropertyJustificationMetadata) metadata.get(key)).toJson());
            } else if (key.equals(PropertySourceMetadata.PROPERTY_SOURCE_METADATA)) {
                json.put(PropertySourceMetadata.PROPERTY_SOURCE_METADATA, ((PropertySourceMetadata) metadata.get(key)).toJson());
            } else {
                json.put(key, metadata.get(key));
            }
        }
        return json;
    }
}
