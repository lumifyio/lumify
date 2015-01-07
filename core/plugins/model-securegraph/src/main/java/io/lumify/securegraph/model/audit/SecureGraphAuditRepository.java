package io.lumify.securegraph.model.audit;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.PropertyJustificationMetadata;
import io.lumify.core.model.PropertySourceMetadata;
import io.lumify.core.model.audit.*;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.core.version.VersionService;
import io.lumify.web.clientapi.model.PropertyType;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.type.GeoPoint;
import org.securegraph.util.IterableUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.lumify.core.model.properties.LumifyProperties.CONCEPT_TYPE;
import static io.lumify.core.model.properties.LumifyProperties.TITLE;

public class SecureGraphAuditRepository extends AuditRepository {
    private final AuditBuilder auditBuilder = new AuditBuilder();
    private final VersionService versionService;
    private final Configuration configuration;
    private final OntologyRepository ontologyRepository;
    private final UserRepository userRepository;

    @Inject
    public SecureGraphAuditRepository(final ModelSession modelSession, final VersionService versionService,
                                      final Configuration configuration, final OntologyRepository ontologyRepository,
                                      final UserRepository userRepository) {
        super(modelSession);
        this.versionService = versionService;
        this.configuration = configuration;
        this.ontologyRepository = ontologyRepository;
        this.userRepository = userRepository;
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

    @Override
    public Iterable<Audit> getAudits(String vertexId, String workspaceId, Authorizations authorizations) {
        ModelUserContext modelUserContext = userRepository.getModelUserContext(authorizations, workspaceId);
        return findByRowStartsWith(vertexId, modelUserContext);
    }

    public Audit createAudit(AuditAction auditAction, Object vertexId, String process, String comment, User user, Visibility visibility) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");
        checkNotNull(process, "process cannot be null");

        Audit audit = new Audit(AuditRowKey.build(vertexId));
        visibility = orVisibility(visibility);
        audit.getAuditCommon()
                .setUser(user, visibility)
                .setAction(auditAction, visibility)
                .setType(OntologyRepository.ENTITY_CONCEPT_IRI, visibility)
                .setComment(comment, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility);

        if (process.length() > 0) {
            audit.getAuditCommon().setProcess(process, visibility);
        }

        return audit;
    }

    @Override
    public Audit auditVertex(AuditAction auditAction, Object vertexId, String process, String comment, User user, Visibility visibility) {
        Audit audit = createAudit(auditAction, vertexId, process, comment, user, visibility);
        save(audit, FlushFlag.FLUSH);
        return audit;
    }

    @Override
    public Audit auditEntityProperty(
            AuditAction action,
            Object id,
            String propertyKey,
            String propertyName,
            Object oldValue,
            Object newValue,
            String process,
            String comment,
            Metadata metadata,
            User user,
            Visibility visibility) {
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
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility);

        audit.getAuditProperty().setPropertyKey(propertyKey != null ? propertyKey : "", visibility);

        if (oldValue != null) {
            if (oldValue instanceof GeoPoint) {
                String val = String.format("POINT(%f,%f)", ((GeoPoint) oldValue).getLatitude(), ((GeoPoint) oldValue).getLongitude());
                audit.getAuditProperty().setPreviousValue(val, visibility);
            } else {
                String convertedValue = checkAndConvertForDateType(propertyName, oldValue);
                audit.getAuditProperty().setPreviousValue(convertedValue != null ? convertedValue : oldValue.toString(), visibility);
            }
        }
        if (action == AuditAction.DELETE) {
            audit.getAuditProperty().setNewValue("", visibility);
        } else {
            if (newValue instanceof GeoPoint) {
                String val = String.format("POINT(%f,%f)", ((GeoPoint) newValue).getLatitude(), ((GeoPoint) newValue).getLongitude());
                audit.getAuditProperty().setNewValue(val, visibility);
            } else {
                String convertedValue = checkAndConvertForDateType(propertyName, newValue);
                audit.getAuditProperty().setNewValue(convertedValue != null ? convertedValue : newValue.toString(), visibility);
            }
        }
        audit.getAuditProperty().setPropertyName(propertyName, visibility);

        if (metadata != null && !metadata.entrySet().isEmpty()) {
            audit.getAuditProperty().setPropertyMetadata(jsonMetadata(metadata).toString(), visibility);
        }

        save(audit);
        return audit;
    }

    @Override
    public List<Audit> auditRelationship(AuditAction action, Vertex sourceVertex, Vertex destVertex, Edge edge, String process,
                                         String comment, User user, Visibility visibility) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(sourceVertex, "sourceVertex cannot be null");
        checkNotNull(destVertex, "destVertex cannot be null");
        checkNotNull(edge, "edge cannot be null");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit auditSourceDest = new Audit(AuditRowKey.build(sourceVertex.getId(), destVertex.getId()));
        Audit auditDestSource = new Audit(AuditRowKey.build(destVertex.getId(), sourceVertex.getId()));
        Audit auditEdge = new Audit(AuditRowKey.build(edge.getId()));
        visibility = orVisibility(visibility);

        List<Audit> audits = new ArrayList<Audit>();
        String displayLabel = ontologyRepository.getDisplayNameForLabel(edge.getLabel());
        checkNotNull(displayLabel, "Could not find display name for label '" + edge.getLabel() + "' on edge " + edge.getId());
        audits.add(auditRelationshipHelper(auditSourceDest, action, sourceVertex, destVertex, displayLabel, process, comment, user, visibility));
        audits.add(auditRelationshipHelper(auditDestSource, action, sourceVertex, destVertex, displayLabel, process, comment, user, visibility));

        auditEdge.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.TYPE_RELATIONSHIP, visibility)
                .setComment(comment, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility);

        Iterable<String> sourceTitleIterable = TITLE.getPropertyValues(sourceVertex);
        String sourceTitle = "";
        if (IterableUtils.count(sourceTitleIterable) != 0) {
            sourceTitle = IterableUtils.toList(sourceTitleIterable).get(IterableUtils.count(sourceTitleIterable) - 1);
        }

        Iterable<String> destTitleIterable = TITLE.getPropertyValues(sourceVertex);
        String destTitle = "";
        if (IterableUtils.count(destTitleIterable) != 0) {
            destTitle = IterableUtils.toList(destTitleIterable).get(IterableUtils.count(destTitleIterable) - 1);
        }

        auditEdge.getAuditRelationship()
                .setSourceId(sourceVertex.getId(), visibility)
                .setSourceType(CONCEPT_TYPE.getPropertyValue(sourceVertex), visibility)
                .setSourceTitle(sourceTitle, visibility)
                .setDestId(destVertex.getId(), visibility)
                .setDestTitle(destTitle, visibility)
                .setDestType(CONCEPT_TYPE.getPropertyValue(destVertex), visibility)
                .setLabel(displayLabel, visibility);

        audits.add(auditEdge);

        saveMany(audits);
        return audits;
    }

    @Override
    public List<Audit> auditRelationshipProperty(AuditAction action, String sourceId, String destId, String propertyKey, String propertyName,
                                                 Object oldValue, Object newValue, Edge edge, String process, String comment, User user,
                                                 Visibility visibility) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(sourceId, "sourceId cannot be null");
        checkNotNull(sourceId.length() > 0, "sourceId cannot be empty");
        checkNotNull(destId, "destId cannot be null");
        checkNotNull(destId.length() > 0, "destId cannot be empty");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkNotNull(propertyName.length() > 0, "propertyName cannot be empty");
        checkNotNull(edge, "edge cannot be null");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit auditSourceDest = new Audit(AuditRowKey.build(sourceId, destId));
        Audit auditDestSource = new Audit(AuditRowKey.build(destId, sourceId));
        Audit auditEdge = new Audit(AuditRowKey.build(edge.getId()));
        visibility = orVisibility(visibility);

        auditSourceDest.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.TYPE_PROPERTY, visibility)
                .setComment(comment, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility);

        auditDestSource.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.TYPE_PROPERTY, visibility)
                .setComment(comment, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility);

        auditEdge.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.TYPE_PROPERTY, visibility)
                .setComment(comment, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility);

        propertyKey = propertyKey != null ? propertyKey : "";

        auditEdge.getAuditProperty().setPropertyKey(propertyKey, visibility);
        auditSourceDest.getAuditProperty().setPropertyKey(propertyKey, visibility);
        auditDestSource.getAuditProperty().setPropertyKey(propertyKey, visibility);

        if (oldValue != null && !oldValue.equals("")) {
            String convertedValue = checkAndConvertForDateType(propertyName, oldValue);
            if (convertedValue != null) {
                oldValue = convertedValue;
            }
            auditDestSource.getAuditProperty().setPreviousValue(oldValue.toString(), visibility);
            auditSourceDest.getAuditProperty().setPreviousValue(oldValue.toString(), visibility);
            auditEdge.getAuditProperty().setPreviousValue(oldValue.toString(), visibility);
        }
        if (action == AuditAction.DELETE) {
            auditDestSource.getAuditProperty().setNewValue("", visibility);
            auditSourceDest.getAuditProperty().setNewValue("", visibility);
            auditEdge.getAuditProperty().setNewValue("", visibility);
        } else {
            String convertedValue = checkAndConvertForDateType(propertyName, newValue);
            if (convertedValue != null) {
                newValue = convertedValue;
            }
            auditDestSource.getAuditProperty().setNewValue(newValue.toString(), visibility);
            auditSourceDest.getAuditProperty().setNewValue(newValue.toString(), visibility);
            auditEdge.getAuditProperty().setNewValue(newValue.toString(), visibility);
        }
        auditDestSource.getAuditProperty().setPropertyName(propertyName, visibility);
        auditSourceDest.getAuditProperty().setPropertyName(propertyName, visibility);
        auditEdge.getAuditProperty().setPropertyName(propertyName, visibility);

        Metadata metadata = edge.getProperty(propertyKey, propertyName).getMetadata();
        if (metadata != null && !metadata.entrySet().isEmpty()) {
            auditDestSource.getAuditProperty().setPropertyMetadata(jsonMetadata(metadata).toString(), visibility);
            auditSourceDest.getAuditProperty().setPropertyMetadata(jsonMetadata(metadata).toString(), visibility);
            auditEdge.getAuditProperty().setPropertyMetadata(jsonMetadata(metadata).toString(), visibility);
        }

        List<Audit> audits = Lists.newArrayList(auditSourceDest, auditDestSource, auditEdge);
        saveMany(audits);
        return audits;
    }

    @Override
    public Audit auditAnalyzedBy(AuditAction action, Vertex vertex, String process, User user, Visibility visibility) {
        Audit audit = new Audit(AuditRowKey.build(vertex.getId()));
        audit.getAuditCommon()
                .setUser(user, visibility)
                .setAction(action, visibility)
                .setType(OntologyRepository.ENTITY_CONCEPT_IRI, visibility)
                .setProcess(process, visibility)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L, visibility)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "", visibility)
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility);

        audit.getAuditEntity().setAnalyzedBy(process, visibility);
        save(audit);
        return audit;
    }

    private Audit auditRelationshipHelper(Audit audit, AuditAction action, Vertex sourceVertex, Vertex destVertex,
                                          String label, String process, String comment, User user,
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
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "", visibility);

        Iterable<String> sourceTitleIterable = TITLE.getPropertyValues(sourceVertex);
        String sourceTitle = "";
        if (IterableUtils.count(sourceTitleIterable) != 0) {
            sourceTitle = IterableUtils.toList(sourceTitleIterable).get(IterableUtils.count(sourceTitleIterable) - 1);
        }

        Iterable<String> destTitleIterable = TITLE.getPropertyValues(destVertex);
        String destTitle = "";
        if (IterableUtils.count(destTitleIterable) != 0) {
            destTitle = IterableUtils.toList(destTitleIterable).get(IterableUtils.count(destTitleIterable) - 1);
        }

        String sourceVertexConceptType = CONCEPT_TYPE.getPropertyValue(sourceVertex);
        checkNotNull(sourceVertexConceptType, "vertex " + sourceVertex.getId() + " has a null " + CONCEPT_TYPE.getPropertyName());
        String destVertexConceptType = CONCEPT_TYPE.getPropertyValue(destVertex);
        checkNotNull(destVertexConceptType, "vertex " + destVertex.getId() + " has a null " + CONCEPT_TYPE.getPropertyName());

        audit.getAuditRelationship()
                .setSourceId(sourceVertex.getId(), visibility)
                .setSourceType(sourceVertexConceptType, visibility)
                .setSourceTitle(sourceTitle, visibility)
                .setDestId(destVertex.getId(), visibility)
                .setDestTitle(destTitle, visibility)
                .setDestType(destVertexConceptType, visibility)
                .setLabel(label, visibility);
        return audit;
    }

    @Override
    public void auditVertexElementMutation(AuditAction action, ElementMutation<Vertex> vertexElementMutation, Vertex vertex, String process,
                                           User user, Visibility visibility) {
        if (vertexElementMutation instanceof ExistingElementMutation) {
            Vertex oldVertex = (Vertex) ((ExistingElementMutation) vertexElementMutation).getElement();
            for (Property property : vertexElementMutation.getProperties()) {
                Object oldPropertyValue = oldVertex.getPropertyValue(property.getKey());
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property (" + property + ") value cannot be null");
                if (!newPropertyValue.equals(oldPropertyValue) || !oldVertex.getVisibility().getVisibilityString().equals(property.getVisibility().getVisibilityString())) {
                    auditEntityProperty(action, oldVertex.getId(), property.getKey(), property.getName(), oldPropertyValue,
                            newPropertyValue, process, "", property.getMetadata(), user, visibility);
                }
            }
        } else {
            auditVertexCreate(vertex.getId(), process, "", user, visibility);
            for (Property property : vertexElementMutation.getProperties()) {
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property (" + property + ") value cannot be null");
                auditEntityProperty(
                        action,
                        vertex.getId(),
                        property.getKey(),
                        property.getName(),
                        null,
                        newPropertyValue,
                        process,
                        "",
                        property.getMetadata(),
                        user,
                        visibility
                );
            }
        }
    }

    @Override
    public void auditEdgeElementMutation(AuditAction action, ElementMutation<Edge> edgeElementMutation, Edge edge, Vertex sourceVertex, Vertex destVertex, String process,
                                         User user, Visibility visibility) {
        if (edgeElementMutation instanceof ExistingElementMutation) {
            Edge oldEdge = (Edge) ((ExistingElementMutation) edgeElementMutation).getElement();
            for (Property property : edgeElementMutation.getProperties()) {
                Object oldPropertyValue = oldEdge.getPropertyValue(property.getKey());
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property value cannot be null");
                if (!newPropertyValue.equals(oldPropertyValue)) {
                    auditRelationshipProperty(action, sourceVertex.getId().toString(), destVertex.getId().toString(), property.getKey(),
                            property.getName(), oldPropertyValue, newPropertyValue, edge, process, "", user, visibility);
                }
            }
        } else {
            auditRelationship(AuditAction.CREATE, sourceVertex, destVertex, edge, process, "", user, visibility);
            for (Property property : edgeElementMutation.getProperties()) {
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property value cannot be null");
                auditRelationshipProperty(action, sourceVertex.getId().toString(), destVertex.getId().toString(), property.getKey(),
                        property.getName(), null, newPropertyValue, edge, process, "", user, visibility);
            }
        }
    }

    @Override
    public void updateColumnVisibility(Audit audit, Visibility originalEdgeVisibility, String visibilityString) {
        getModelSession().alterColumnsVisibility(audit, orVisibility(originalEdgeVisibility).getVisibilityString(), visibilityString, FlushFlag.FLUSH);
    }

    private JSONObject jsonMetadata(Metadata metadata) {
        JSONObject json = new JSONObject();
        for (Metadata.Entry metadataEntry : metadata.entrySet()) {
            if (metadataEntry.getKey().equals(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION)) {
                json.put(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION, ((PropertyJustificationMetadata) metadataEntry.getValue()).toJson());
            } else if (metadataEntry.getKey().equals(PropertySourceMetadata.PROPERTY_SOURCE_METADATA)) {
                json.put(PropertySourceMetadata.PROPERTY_SOURCE_METADATA, ((PropertySourceMetadata) metadataEntry.getValue()).toJson());
            } else {
                json.put(metadataEntry.getKey(), metadataEntry.getValue());
            }
        }
        return json;
    }

    private Visibility orVisibility(Visibility visibility) {
        String auditVisibilityLabel = configuration.get(Configuration.AUDIT_VISIBILITY_LABEL, null);
        if (auditVisibilityLabel != null && !visibility.toString().equals("")) {
            if (visibility.toString().equals(auditVisibilityLabel)) {
                return new Visibility(auditVisibilityLabel);
            }
            return new Visibility("(" + auditVisibilityLabel + "|" + visibility.toString() + ")");
        }
        return visibility;
    }

    private String checkAndConvertForDateType(String propertyName, Object value) {
        OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(propertyName);
        if (ontologyProperty != null && ontologyProperty.getDataType() == PropertyType.DATE) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyy");
            try {
                return String.valueOf(dateFormat.parse(value.toString()).getTime());
            } catch (ParseException e) {
                throw new RuntimeException("could not parse date");
            }
        }
        return null;
    }

    private Audit auditVertexCreate(Object vertexId, String process, String comment, User user, Visibility visibility) {
        return auditVertex(AuditAction.CREATE, vertexId, process, comment, user, visibility);
    }
}
