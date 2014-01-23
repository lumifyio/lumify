package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.util.List;

import static com.altamiracorp.lumify.core.util.CollectionUtil.single;
import static com.google.common.base.Preconditions.checkNotNull;

public class EntityHelper {
    private static final Visibility DEFAULT_VISIBILITY = new Visibility("");
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;
    private final WorkQueueRepository workQueueRepository;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public EntityHelper(final TermMentionRepository termMentionRepository,
                        final Graph graph,
                        final WorkQueueRepository workQueueRepository,
                        final AuditRepository auditRepository,
                        final OntologyRepository ontologyRepository) {
        this.termMentionRepository = termMentionRepository;
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
    }

    public void updateTermMention(TermMentionModel termMention, String sign, Concept concept, Vertex resolvedVertex, User user) {
        checkNotNull(concept, "conceptVertex cannot be null");
        termMention.getMetadata()
                .setSign(sign)
                .setOntologyClassUri(concept.getDisplayName())
                .setConceptGraphVertexId(concept.getId())
                .setVertexId(resolvedVertex.getId().toString());
        termMentionRepository.save(termMention, user.getModelUserContext());
    }

    public void updateGraphVertex(Vertex vertex, String subType, String title, String process, String comment, User user) {
        vertex.prepareMutation()
                .setProperty(PropertyName.CONCEPT_TYPE.toString(), subType, DEFAULT_VISIBILITY)
                .setProperty(PropertyName.TITLE.toString(), title, DEFAULT_VISIBILITY)
                .save();

        auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), vertex, PropertyName.CONCEPT_TYPE.toString(), process, comment, user);
        auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), vertex, PropertyName.TITLE.toString(), process, comment, user);
    }

    public ArtifactDetectedObject createObjectTag(String x1, String x2, String y1, String y2, Vertex resolvedVertex, Vertex conceptVertex) {
        String concept;
        if (conceptVertex.getPropertyValue("ontologyTitle", 0).toString().equals("person")) {
            concept = "face";
        } else {
            concept = conceptVertex.getPropertyValue("ontologyTitle", 0).toString();
        }

        ArtifactDetectedObject detectedObject = new ArtifactDetectedObject(x1, y1, x2, y2, concept);
        detectedObject.setGraphVertexId(resolvedVertex.getId().toString());

        detectedObject.setResolvedVertex(resolvedVertex);

        return detectedObject;
    }

    public void scheduleHighlight(String artifactGraphVertexId, User user) {
        workQueueRepository.pushUserArtifactHighlight(artifactGraphVertexId);
    }

    public Vertex createGraphVertex(Vertex conceptVertex, String sign, String existing, String process, String comment,
                                    String artifactId, User user) {
        boolean newVertex = false;
        List<String> modifiedProperties = Lists.newArrayList(PropertyName.CONCEPT_TYPE.toString(), PropertyName.TITLE.toString());
        final Vertex artifactVertex = graph.getVertex(artifactId, user.getAuthorizations());
        Vertex resolvedVertex;
        // If the user chose to use an existing resolved entity
        if (existing != null && !existing.isEmpty()) {
            // TODO what happens if we have multiple vertices with the same title
            resolvedVertex = single(graph.query(user.getAuthorizations())
                    .has(PropertyName.TITLE.toString(), sign)
                    .vertices());
        } else {
            newVertex = true;
            resolvedVertex = graph.addVertex(DEFAULT_VISIBILITY, user.getAuthorizations());
        }

        String conceptId = conceptVertex.getId().toString();
        resolvedVertex.prepareMutation()
                .setProperty(PropertyName.CONCEPT_TYPE.toString(), conceptId, DEFAULT_VISIBILITY)
                .setProperty(PropertyName.TITLE.toString(), sign, DEFAULT_VISIBILITY)
                .save();

        if (newVertex) {
            auditRepository.auditEntity(AuditAction.CREATE.toString(), resolvedVertex.getId(), artifactId, sign, conceptId, process, comment, user);
        }

        for (String modifiedProperty : modifiedProperties) {
            auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), resolvedVertex, modifiedProperty, process, comment, user);
        }

        graph.addEdge(artifactVertex, resolvedVertex, LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString(), DEFAULT_VISIBILITY, user.getAuthorizations());
        String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString());
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationships(AuditAction.CREATE.toString(), artifactVertex, resolvedVertex, labelDisplayName, "", "", user);

        return resolvedVertex;
    }

    public JSONObject formatUpdatedArtifactVertexProperty(Object id, String propertyKey, Object propertyValue) {
        // puts the updated artifact vertex property in the correct JSON format

        JSONObject artifactVertexProperty = new JSONObject();
        artifactVertexProperty.put("id", id);

        JSONObject properties = new JSONObject();
        properties.put(propertyKey, propertyValue);

        artifactVertexProperty.put("properties", properties);
        return artifactVertexProperty;
    }
}
