package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.graph.InMemoryGraphVertex;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.User;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.util.List;

public class EntityHelper {
    private final GraphRepository graphRepository;
    private final TermMentionRepository termMentionRepository;
    private final WorkQueueRepository workQueueRepository;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public EntityHelper(final TermMentionRepository termMentionRepository,
                        final GraphRepository graphRepository,
                        final WorkQueueRepository workQueueRepository,
                        final AuditRepository auditRepository,
                        final OntologyRepository ontologyRepository) {
        this.termMentionRepository = termMentionRepository;
        this.graphRepository = graphRepository;
        this.workQueueRepository = workQueueRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
    }

    public void updateTermMention(TermMentionModel termMention, String sign, GraphVertex conceptVertex, GraphVertex resolvedVertex, User user) {
        termMention.getMetadata()
                .setSign(sign)
                .setOntologyClassUri((String) conceptVertex.getProperty(PropertyName.DISPLAY_NAME))
                .setConceptGraphVertexId(conceptVertex.getId())
                .setGraphVertexId(resolvedVertex.getId());
        termMentionRepository.save(termMention, user.getModelUserContext());
    }

    public void updateGraphVertex(GraphVertex vertex, String subType, String title, String process, String comment, User user) {
        vertex.setProperty(PropertyName.CONCEPT_TYPE, subType);
        vertex.setProperty(PropertyName.TITLE, title);
        graphRepository.saveVertex(vertex, user);

        auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), vertex, PropertyName.CONCEPT_TYPE.toString(), process, comment, user);
        auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), vertex, PropertyName.TITLE.toString(), process, comment, user);
    }

    public ArtifactDetectedObject createObjectTag(String x1, String x2, String y1, String y2, GraphVertex resolvedVertex, GraphVertex conceptVertex) {
        String concept;
        if (conceptVertex.getProperty("ontologyTitle").toString().equals("person")) {
            concept = "face";
        } else {
            concept = conceptVertex.getProperty("ontologyTitle").toString();
        }

        ArtifactDetectedObject detectedObject = new ArtifactDetectedObject(x1, y1, x2, y2, concept);
        detectedObject.setGraphVertexId(resolvedVertex.getId().toString());

        detectedObject.setResolvedVertex(resolvedVertex);

        return detectedObject;
    }

    public void scheduleHighlight(String artifactGraphVertexId, User user) {
        workQueueRepository.pushUserArtifactHighlight(artifactGraphVertexId);
    }

    public GraphVertex createGraphVertex(GraphVertex conceptVertex, String sign, String existing, String process, String comment,
                                         String artifactId, User user) {
        boolean newVertex = false;
        List<String> modifiedProperties = Lists.newArrayList(PropertyName.CONCEPT_TYPE.toString(), PropertyName.TITLE.toString());
        final GraphVertex artifactVertex = graphRepository.findVertex(artifactId, user);
        GraphVertex resolvedVertex;
        // If the user chose to use an existing resolved entity
        if (existing != null && !existing.isEmpty()) {
            resolvedVertex = graphRepository.findVertexByExactTitle(sign, user);
        } else {
            newVertex = true;
            resolvedVertex = new InMemoryGraphVertex();
        }

        String conceptId = conceptVertex.getId();
        resolvedVertex.setProperty(PropertyName.CONCEPT_TYPE, conceptId);
        resolvedVertex.setProperty(PropertyName.TITLE, sign);

        graphRepository.saveVertex(resolvedVertex, user);

        if (newVertex) {
            auditRepository.auditEntity(AuditAction.CREATE.toString(), resolvedVertex.getId(), artifactId, sign, conceptId, process, comment, user);
        }

        for (String modifiedProperty : modifiedProperties) {
            auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), resolvedVertex, modifiedProperty, process, comment, user);
        }

        graphRepository.saveRelationship(artifactId, resolvedVertex.getId(), LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY, user);
        String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString(), user);
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationships(AuditAction.CREATE.toString(), artifactVertex, resolvedVertex, labelDisplayName, "", "", user);

        return resolvedVertex;
    }

    public JSONObject formatUpdatedArtifactVertexProperty(String id, String propertyKey, Object propertyValue) {
        // puts the updated artifact vertex property in the correct JSON format

        JSONObject artifactVertexProperty = new JSONObject();
        artifactVertexProperty.put("id", id);

        JSONObject properties = new JSONObject();
        properties.put(propertyKey, propertyValue);

        artifactVertexProperty.put("properties", properties);
        return artifactVertexProperty;
    }
}
