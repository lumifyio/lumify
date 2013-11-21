package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.graph.InMemoryGraphVertex;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.VertexType;
import com.altamiracorp.lumify.core.model.termMention.TermMention;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.User;
import com.google.inject.Inject;
import org.json.JSONObject;

public class EntityHelper {
    private final GraphRepository graphRepository;
    private final TermMentionRepository termMentionRepository;
    private final WorkQueueRepository workQueueRepository;
    private final AuditRepository auditRepository;

    @Inject
    public EntityHelper(final TermMentionRepository termMentionRepository,
                        final GraphRepository graphRepository, WorkQueueRepository workQueueRepository,
                        final AuditRepository auditRepository) {
        this.termMentionRepository = termMentionRepository;
        this.graphRepository = graphRepository;
        this.workQueueRepository = workQueueRepository;
        this.auditRepository = auditRepository;
    }

    public void updateTermMention(TermMention termMention, String sign, GraphVertex conceptVertex, GraphVertex resolvedVertex, User user) {
        termMention.getMetadata()
                .setSign(sign)
                .setOntologyClassUri((String) conceptVertex.getProperty(PropertyName.DISPLAY_NAME))
                .setConceptGraphVertexId(conceptVertex.getId())
                .setGraphVertexId(resolvedVertex.getId());
        termMentionRepository.save(termMention, user.getModelUserContext());
    }

    public void updateGraphVertex(GraphVertex vertex, String subType, String title, User user) {
        auditRepository.audit(vertex.getId(), auditRepository.updateEntityAuditMessage(), user);
        vertex.setProperty(PropertyName.SUBTYPE, subType);
        auditRepository.audit(vertex.getId(), auditRepository.propertyAuditMessage(vertex, PropertyName.SUBTYPE.toString(), subType), user);
        vertex.setProperty(PropertyName.TITLE, title);
        auditRepository.audit(vertex.getId(), auditRepository.propertyAuditMessage(vertex, PropertyName.TITLE.toString(), title), user);

        graphRepository.saveVertex(vertex, user);
    }

    public ArtifactDetectedObject createObjectTag(String x1, String x2, String y1, String y2, GraphVertex resolvedVertex, GraphVertex conceptVertex) {
        ArtifactDetectedObject detectedObject = new ArtifactDetectedObject(x1, y1, x2, y2);
        detectedObject.setGraphVertexId(resolvedVertex.getId().toString());

        if (conceptVertex.getProperty("ontologyTitle").toString().equals("person")) {
            detectedObject.setConcept("face");
        } else {
            detectedObject.setConcept(conceptVertex.getProperty("ontologyTitle").toString());
        }
        detectedObject.setResolvedVertex(resolvedVertex);

        return detectedObject;
    }

    public void scheduleHighlight(String artifactGraphVertexId, User user) {
        workQueueRepository.pushArtifactHighlight(artifactGraphVertexId);
    }

    public GraphVertex createGraphVertex(GraphVertex conceptVertex, String sign, String existing, String boundingBox,
                                          String artifactId, User user) {
        GraphVertex resolvedVertex;
        // If the user chose to use an existing resolved entity
        if( existing != null && !existing.isEmpty() ) {
            resolvedVertex = graphRepository.findVertexByTitleAndType(sign, VertexType.ENTITY, user);
            auditRepository.audit(resolvedVertex.getId(), auditRepository.updateEntityAuditMessage(), user);
        } else {
            resolvedVertex = new InMemoryGraphVertex();
            auditRepository.audit(resolvedVertex.getId(), auditRepository.createEntityAuditMessage() , user);

            resolvedVertex.setType(VertexType.ENTITY);
            auditRepository.audit(resolvedVertex.getId(), auditRepository.propertyAuditMessage(resolvedVertex, PropertyName.TYPE.toString(), VertexType.ENTITY.toString()), user);
        }

        String conceptId = conceptVertex.getId();
        resolvedVertex.setProperty(PropertyName.SUBTYPE, conceptId);
        auditRepository.audit(resolvedVertex.getId(), auditRepository.propertyAuditMessage(resolvedVertex, PropertyName.SUBTYPE.toString(), conceptId), user);

        resolvedVertex.setProperty(PropertyName.TITLE, sign);
        auditRepository.audit(resolvedVertex.getId(), auditRepository.propertyAuditMessage(resolvedVertex, PropertyName.TITLE.toString(), sign), user);

        graphRepository.saveVertex(resolvedVertex, user);

        graphRepository.saveRelationship(artifactId, resolvedVertex.getId(), LabelName.CONTAINS_IMAGE_OF, user);
        graphRepository.setPropertyEdge(artifactId, resolvedVertex.getId(), LabelName.CONTAINS_IMAGE_OF.toString()
                , PropertyName.BOUNDING_BOX.toString(), boundingBox, user);
        return resolvedVertex;
    }

    public JSONObject formatUpdatedArtifactVertexProperty (String id, String propertyKey, Object propertyValue) {
        // puts the updated artifact vertex property in the correct JSON format

        JSONObject artifactVertexProperty = new JSONObject();
        artifactVertexProperty.put("id", id);

        JSONObject properties = new JSONObject();
        properties.put(propertyKey, propertyValue);

        artifactVertexProperty.put("properties", properties);
        return artifactVertexProperty;
    }
}
