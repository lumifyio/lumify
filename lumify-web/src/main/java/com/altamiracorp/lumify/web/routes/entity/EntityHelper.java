package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONObject;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;
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

    public void updateTermMention(TermMentionModel termMention, String sign, Concept concept, Vertex resolvedVertex, Visibility visibility, User user) {
        checkNotNull(concept, "conceptVertex cannot be null");
        termMention.getMetadata()
                .setSign(sign, visibility)
                .setOntologyClassUri(concept.getDisplayName(), visibility)
                .setConceptGraphVertexId(concept.getId(), visibility)
                .setVertexId(resolvedVertex.getId().toString(), visibility);
        termMentionRepository.save(termMention);
    }

    public ElementMutation<Vertex> updateMutation(ElementMutation<Vertex> vertexMutation, String subType, String title, String process,
                                                  String comment, User user) {
        CONCEPT_TYPE.setProperty(vertexMutation, subType, DEFAULT_VISIBILITY);
        TITLE.setProperty(vertexMutation, title, DEFAULT_VISIBILITY);
        return vertexMutation;
    }

    public ArtifactDetectedObject createObjectTag(String x1, String x2, String y1, String y2, Vertex resolvedVertex, Concept concept) {
        String conceptName;
        if (concept.getVertex().getPropertyValue("ontologyTitle", 0).toString().equals("person")) {
            conceptName = "face";
        } else {
            conceptName = concept.getVertex().getPropertyValue("ontologyTitle", 0).toString();
        }

        ArtifactDetectedObject detectedObject = new ArtifactDetectedObject(x1, y1, x2, y2, conceptName);
        detectedObject.setGraphVertexId(resolvedVertex.getId().toString());

        detectedObject.setResolvedVertex(resolvedVertex);

        return detectedObject;
    }

    public ElementMutation<Vertex> createGraphMutation(Concept concept, String sign, String existing, Object graphVertexId, String process, String comment,
                                                       Authorizations authorizations) {
        ElementMutation<Vertex> resolvedVertexMutation;
        // If the user chose to use an existing resolved entity
        if (existing != null && Boolean.valueOf(existing)) {
            resolvedVertexMutation = graph.getVertex(graphVertexId, authorizations).prepareMutation();
        } else {
            resolvedVertexMutation = graph.prepareVertex(DEFAULT_VISIBILITY, authorizations);
        }

        CONCEPT_TYPE.setProperty(resolvedVertexMutation, concept.getId(), DEFAULT_VISIBILITY);
        TITLE.setProperty(resolvedVertexMutation, sign, DEFAULT_VISIBILITY);

        return resolvedVertexMutation;
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
