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
import com.altamiracorp.securegraph.ElementMutation;
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

    public ElementMutation<Vertex> updateMutation(ElementMutation<Vertex> vertexMutation, String subType, String title, String process, String comment, User user) {
        vertexMutation.setProperty(PropertyName.CONCEPT_TYPE.toString(), subType, DEFAULT_VISIBILITY)
            .setProperty(PropertyName.TITLE.toString(), title, DEFAULT_VISIBILITY);
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

    public void scheduleHighlight(String artifactGraphVertexId, User user) {
        workQueueRepository.pushUserArtifactHighlight(artifactGraphVertexId);
    }

    public ElementMutation<Vertex> createGraphMutation(Concept concept, String sign, String existing, Object graphVertexId, String process, String comment,
                                      User user) {
        ElementMutation<Vertex> resolvedVertexMutation;
        // If the user chose to use an existing resolved entity
        if (existing != null && Boolean.valueOf(existing)) {
            resolvedVertexMutation = graph.getVertex(graphVertexId, user.getAuthorizations()).prepareMutation();
        } else {
            resolvedVertexMutation = graph.prepareVertex(DEFAULT_VISIBILITY, user.getAuthorizations());
        }

        String conceptId = concept.getId().toString();
        resolvedVertexMutation.setProperty(PropertyName.CONCEPT_TYPE.toString(), conceptId, DEFAULT_VISIBILITY)
                .setProperty(PropertyName.TITLE.toString(), sign, DEFAULT_VISIBILITY);

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
