package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.PropertyJustificationMetadata;
import com.altamiracorp.lumify.core.model.PropertySourceMetadata;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.version.VersionService;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
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
    private final Configuration configuration;

    @Inject
    public AuditRepository(final ModelSession modelSession, final VersionService versionService, final Configuration configuration) {
        super(modelSession);
        this.versionService = versionService;
        this.configuration = configuration;
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

    public Audit auditVertexCreate(Object vertexId, String process, String comment, User user, String isPublished, Visibility visibility) {
        return auditVertex(AuditAction.CREATE, vertexId, process, comment, user, FlushFlag.DEFAULT, isPublished, visibility);
    }

    public Audit auditVertex(AuditAction auditAction, Object vertexId, String process, String comment, User user, FlushFlag flushFlag, String isPublished, Visibility visibility) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");
        checkNotNull(process, "process cannot be null");

        Audit audit = new Audit(AuditRowKey.build(vertexId));
        visibility = orVisibility(visibility);
        audit.getAuditCommon()
                .setUser(user, visibility)
                .setAction(auditAction, visibility)
                .setType(OntologyRepository.TYPE_ENTITY, visibility)
                .setComment(comment, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility)
                .setPublished(isPublished, visibility);

        if (process.length() > 0) {
            audit.getAuditCommon().setProcess(process, visibility);
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
            User user,
            boolean isPublished,
            Visibility visibility) {
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
        visibility = orVisibility(visibility);

        List<Audit> audits = new ArrayList<Audit>();
        audits.add(auditEntityHelper(auditEntity, action, entityId, entityTitle, entitySubtype, process, comment, user, isPublished, visibility));
        audits.add(auditEntityHelper(auditArtifact, action, entityId, entityTitle, entitySubtype, process, comment, user, isPublished, visibility));
        saveMany(audits);
        return audits;
    }

    public Audit auditEntityProperty(AuditAction action, Object id, String propertyName, Object oldValue, Object newValue,
                                     String process, String comment, Map<String, Object> metadata, User user,
                                     String isPublished, Visibility visibility) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(id, "id cannot be null");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkArgument(propertyName.length() > 0, "property name cannot be empty");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit audit = new Audit(AuditRowKey.build(id));
        visibility = orVisibility(visibility);

        audit.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.TYPE_PROPERTY, visibility)
                .setComment(comment, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility)
                .setPublished(isPublished, visibility);

        if (oldValue != null) {
            if (oldValue instanceof GeoPoint) {
                String val = String.format("POINT(%f,%f)", ((GeoPoint) oldValue).getLatitude(), ((GeoPoint) oldValue).getLongitude());
                audit.getAuditProperty().setPreviousValue(val, visibility);
            } else {
                audit.getAuditProperty().setPreviousValue(oldValue.toString(), visibility);
            }
        }
        if (action == AuditAction.DELETE) {
            audit.getAuditProperty().setNewValue("", visibility);
        } else {
            if (newValue instanceof GeoPoint) {
                String val = String.format("POINT(%f,%f)", ((GeoPoint) newValue).getLatitude(), ((GeoPoint) newValue).getLongitude());
                audit.getAuditProperty().setNewValue(val, visibility);
            } else {
                audit.getAuditProperty().setNewValue(newValue.toString(), visibility);
            }
        }
        audit.getAuditProperty().setPropertyName(propertyName, visibility);

        if (metadata != null || !metadata.isEmpty()) {
            audit.getAuditProperty().setPropertyMetadata(jsonMetadata(metadata).toString(), visibility);
        }

        save(audit);
        return audit;
    }

    public List<Audit> auditRelationship(AuditAction action, Vertex sourceVertex, Vertex destVertex, String label,
                                         String process, String comment, User user, String isPublished, Visibility visibility) {
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
        visibility = orVisibility(visibility);

        List<Audit> audits = new ArrayList<Audit>();
        audits.add(auditRelationshipHelper(auditSourceDest, action, sourceVertex, destVertex, label, process, comment, user, isPublished, visibility));
        audits.add(auditRelationshipHelper(auditDestSource, action, sourceVertex, destVertex, label, process, comment, user, isPublished, visibility));
        saveMany(audits);
        return audits;
    }

    public List<Audit> auditRelationshipProperty(AuditAction action, String sourceId, String destId, String propertyName,
                                                 Object oldValue, Edge edge, String process, String comment, User user,
                                                 String isPublished, Visibility visibility) {
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
        visibility = orVisibility(visibility);

        auditSourceDest.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.TYPE_PROPERTY, visibility)
                .setComment(comment, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility)
                .setPublished(isPublished, visibility);

        auditDestSource.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.TYPE_PROPERTY, visibility)
                .setComment(comment, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility)
                .setPublished(isPublished, visibility);

        if (!oldValue.equals("")) {
            auditDestSource.getAuditProperty().setPreviousValue(oldValue, visibility);
            auditSourceDest.getAuditProperty().setPreviousValue(oldValue, visibility);
        }
        if (action == AuditAction.DELETE) {
            auditDestSource.getAuditProperty().setNewValue("", visibility);
            auditSourceDest.getAuditProperty().setNewValue("", visibility);
        } else {
            // TODO handle multi-valued properties
            auditDestSource.getAuditProperty().setNewValue(edge.getPropertyValue(propertyName, 0), visibility);
            auditSourceDest.getAuditProperty().setNewValue(edge.getPropertyValue(propertyName, 0), visibility);
        }
        auditDestSource.getAuditProperty().setPropertyName(propertyName, visibility);
        auditSourceDest.getAuditProperty().setPropertyName(propertyName, visibility);

        List<Audit> audits = Lists.newArrayList(auditSourceDest, auditDestSource);
        saveMany(audits);
        return audits;
    }

    private Audit auditEntityHelper(Audit audit, AuditAction action, Object entityID, String entityTitle,
                                    String entitySubtype, String process, String comment, User user, boolean isPublished,
                                    Visibility visibility) {
        visibility = orVisibility(visibility);
        audit.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.TYPE_ENTITY, visibility)
                .setComment(comment, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility)
                .setPublished(String.valueOf(isPublished), visibility);

        audit.getAuditEntity()
                .setTitle(entityTitle, visibility)
                .setType(OntologyRepository.TYPE_ENTITY, visibility)
                .setSubtype(entitySubtype, visibility)
                .setID(entityID.toString(), visibility);
        return audit;
    }

    private Audit auditRelationshipHelper(Audit audit, AuditAction action, Vertex sourceVertex, Vertex destVertex,
                                          String label, String process, String comment, User user, String isPublished,
                                          Visibility visibility) {
        visibility = orVisibility(visibility);
        audit.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.TYPE_RELATIONSHIP, visibility)
                .setComment(comment, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility)
                .setPublished(isPublished, visibility);

        audit.getAuditRelationship()
                .setSourceId(sourceVertex.getId(), visibility)
                .setSourceType(CONCEPT_TYPE.getPropertyValue(sourceVertex), visibility)
                .setSourceTitle(TITLE.getPropertyValue(sourceVertex), visibility)
                .setDestId(destVertex.getId(), visibility)
                .setDestTitle(TITLE.getPropertyValue(destVertex), visibility)
                .setDestType(CONCEPT_TYPE.getPropertyValue(destVertex), visibility)
                .setLabel(label, visibility);
        return audit;
    }

    public void auditVertexElementMutation(ElementMutation<Vertex> vertexElementMutation, Vertex vertex, String process,
                                           User user, String isPublished, Visibility visibility) {
        if (vertexElementMutation instanceof ExistingElementMutation) {
            Vertex oldVertex = (Vertex) ((ExistingElementMutation) vertexElementMutation).getElement();
            for (Property property : vertexElementMutation.getProperties()) {
                // TODO handle multi-valued properties
                Object oldPropertyValue = oldVertex.getPropertyValue(property.getName());
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property value cannot be null");
                if (!newPropertyValue.equals(oldPropertyValue)) {
                    auditEntityProperty(AuditAction.UPDATE, oldVertex.getId(), property.getName(), oldPropertyValue,
                            newPropertyValue, process, "", property.getMetadata(), user, isPublished,  visibility);
                }
            }
        } else {
            auditVertexCreate(vertex.getId(), process, "", user, isPublished,  visibility);
            for (Property property : vertexElementMutation.getProperties()) {
                // TODO handle multi-valued properties
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property value cannot be null");
                auditEntityProperty(AuditAction.UPDATE, vertex.getId(), property.getName(), null, newPropertyValue, process, "",
                        property.getMetadata(), user, isPublished, visibility);
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

    private Visibility orVisibility(Visibility visibility) {
        if (!configuration.get(Configuration.AUDIT_VISIBILITY_LABEL).equals(Configuration.UNKNOWN_STRING) && !visibility.toString().equals("")) {
            String auditVisibility = configuration.get(Configuration.AUDIT_VISIBILITY_LABEL);
            if (visibility.toString().equals(auditVisibility)) {
                return new Visibility(auditVisibility);
            }
            return new Visibility("(" + auditVisibility + "|" + visibility.toString() + ")");
        }
        return visibility;
    }
}
