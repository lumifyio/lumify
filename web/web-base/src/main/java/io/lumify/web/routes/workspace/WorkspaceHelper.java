package io.lumify.web.routes.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.*;

import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

@Singleton
public class WorkspaceHelper {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceHelper.class);
    private final TermMentionRepository termMentionRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;
    private final WorkQueueRepository workQueueRepository;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final String entityHasImageIri;
    private final String artifactContainsImageOfEntityIri;

    @Inject
    public WorkspaceHelper(
            final TermMentionRepository termMentionRepository,
            final Configuration configuration,
            final AuditRepository auditRepository,
            final UserRepository userRepository,
            final WorkQueueRepository workQueueRepository,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator) {
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.workQueueRepository = workQueueRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;

        this.entityHasImageIri = configuration.get(Configuration.ONTOLOGY_IRI_ENTITY_HAS_IMAGE);
        if (this.entityHasImageIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ENTITY_HAS_IMAGE);
        }

        this.artifactContainsImageOfEntityIri = configuration.get(Configuration.ONTOLOGY_IRI_ARTIFACT_CONTAINS_IMAGE_OF_ENTITY);
        if (this.artifactContainsImageOfEntityIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ARTIFACT_CONTAINS_IMAGE_OF_ENTITY);
        }
    }

    public void unresolveTerm(Vertex resolvedVertex, Vertex termMention, LumifyVisibility visibility, User user, Authorizations authorizations) {
        Vertex sourceVertex = termMentionRepository.findSourceVertex(termMention, authorizations);
        if (sourceVertex == null) {
            return;
        }
        List<Edge> edges = toList(sourceVertex.getEdges(Direction.BOTH, authorizations));

        if (edges.size() == 1) {
            graph.removeEdge(edges.get(0), authorizations);
            workQueueRepository.pushEdgeDeletion(edges.get(0));
            auditRepository.auditRelationship(AuditAction.DELETE, sourceVertex, resolvedVertex, edges.get(0), "", "", user, visibility.getVisibility());
        }

        termMentionRepository.delete(termMention, authorizations);
        workQueueRepository.pushTextUpdated(sourceVertex.getId());

        graph.flush();

        auditRepository.auditVertex(AuditAction.UNRESOLVE, resolvedVertex.getId(), "", "", user, visibility.getVisibility());
    }

    public void deleteProperty(Vertex vertex, Property property, boolean propertyIsPublic, String workspaceId, User user, Authorizations authorizations) {
        auditRepository.auditEntityProperty(AuditAction.DELETE, vertex.getId(), property.getKey(), property.getName(), property.getValue(), null, "", "", property.getMetadata(), user, property.getVisibility());

        if (propertyIsPublic) {
            vertex.markPropertyHidden(property, new Visibility(workspaceId), authorizations);
        } else {
            vertex.removeProperty(property.getKey(), property.getName(), authorizations);
        }

        graph.flush();

        workQueueRepository.pushGraphPropertyQueue(vertex, property);
    }

    public void deleteEdge(String workspaceId, Edge edge, Vertex sourceVertex, Vertex destVertex, boolean isPublicEdge, User user, Authorizations authorizations) {
        if (isPublicEdge) {
            Visibility workspaceVisibility = new Visibility(workspaceId);

            graph.markEdgeHidden(edge, workspaceVisibility, authorizations);

            if (edge.getLabel().equals(entityHasImageIri)) {
                Property entityHasImage = sourceVertex.getProperty(LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName());
                sourceVertex.markPropertyHidden(entityHasImage, workspaceVisibility, authorizations);
                this.workQueueRepository.pushElementImageQueue(sourceVertex, entityHasImage);
            }

            for (Vertex termMention : termMentionRepository.findByEdgeId(sourceVertex.getId(), edge.getId(), authorizations)) {
                termMentionRepository.markHidden(termMention, workspaceVisibility, authorizations);
                workQueueRepository.pushTextUpdated(sourceVertex.getId());
            }
        } else {
            graph.removeEdge(edge, authorizations);

            if (edge.getLabel().equals(entityHasImageIri)) {
                Property entityHasImage = sourceVertex.getProperty(LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName());
                sourceVertex.removeProperty(entityHasImage.getName(), authorizations);
                this.workQueueRepository.pushElementImageQueue(sourceVertex, entityHasImage);
            }

            for (Vertex termMention : termMentionRepository.findByEdgeId(sourceVertex.getId(), edge.getId(), authorizations)) {
                termMentionRepository.delete(termMention, authorizations);
                workQueueRepository.pushTextUpdated(sourceVertex.getId());
            }

            this.workQueueRepository.pushEdgeDeletion(edge);

            // TODO: replace "" when we implement commenting on ui
            auditRepository.auditRelationship(AuditAction.DELETE, sourceVertex, destVertex, edge, "", "", user, new LumifyVisibility().getVisibility());
        }

        graph.flush();
    }

    public void deleteVertex(Vertex vertex, String workspaceId, boolean isPublicEdge, Authorizations authorizations, User user) {
        if (isPublicEdge) {
            Visibility workspaceVisibility = new Visibility(workspaceId);

            graph.markVertexHidden(vertex, workspaceVisibility, authorizations);
        } else {
            JSONArray unresolved = new JSONArray();
            VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(vertex);
            visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJson);
            LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

            // because we store the current vertex image in a property we need to possibly find that property and change it
            //  if we are deleting the current image.
            for (Edge edge : vertex.getEdges(Direction.BOTH, entityHasImageIri, authorizations)) {
                if (edge.getVertexId(Direction.IN).equals(vertex.getId())) {
                    Vertex outVertex = edge.getVertex(Direction.OUT, authorizations);
                    Property entityHasImage = outVertex.getProperty(LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName());
                    outVertex.removeProperty(entityHasImage.getName(), authorizations);
                    workQueueRepository.pushElementImageQueue(outVertex, entityHasImage);
                }
            }

            // because detected objects are currently stored as properties on the artifact that reference the entity
            //   that they are resolved to we need to delete that property
            for (Edge edge : vertex.getEdges(Direction.BOTH, artifactContainsImageOfEntityIri, authorizations)) {
                for (Property rowKeyProperty : vertex.getProperties(LumifyProperties.ROW_KEY.getPropertyName())) {
                    String multiValueKey = rowKeyProperty.getValue().toString();
                    if (edge.getVertexId(Direction.IN).equals(vertex.getId())) {
                        Vertex outVertex = edge.getVertex(Direction.OUT, authorizations);
                        // remove property
                        LumifyProperties.DETECTED_OBJECT.removeProperty(outVertex, multiValueKey, authorizations);
                        graph.removeEdge(edge, authorizations);
                        auditRepository.auditRelationship(AuditAction.DELETE, outVertex, vertex, edge, "", "", user, lumifyVisibility.getVisibility());
                        workQueueRepository.pushEdgeDeletion(edge);
                        workQueueRepository.pushGraphPropertyQueue(outVertex, multiValueKey,
                                LumifyProperties.DETECTED_OBJECT.getPropertyName(), workspaceId, visibilityJson.getSource());
                    }
                }
            }

            // because we store term mentions with an added visibility we need to delete them with that added authorizations.
            //  we also need to notify the front-end of changes as well as audit the changes
            for (Vertex termMention : termMentionRepository.findResolvedTo(vertex.getId(), authorizations)) {
                unresolveTerm(vertex, termMention, lumifyVisibility, user, authorizations);
                JSONObject result = new JSONObject();
                result.put("success", true);
                unresolved.put(result);
            }

            // because we store workspaces with an added visibility we need to delete them with that added authorizations.
            Authorizations systemAuthorization = userRepository.getAuthorizations(user, WorkspaceRepository.VISIBILITY_STRING, workspaceId);
            Vertex workspaceVertex = graph.getVertex(workspaceId, systemAuthorization);
            for (Edge edge : workspaceVertex.getEdges(vertex, Direction.BOTH, systemAuthorization)) {
                graph.removeEdge(edge, systemAuthorization);
            }

            graph.removeVertex(vertex, authorizations);
            graph.flush();
            this.workQueueRepository.pushVertexDeletion(vertex);

            // TODO: replace "" when we implement commenting on ui
            auditRepository.auditVertex(AuditAction.DELETE, vertex.getId(), "", "", user, new LumifyVisibility().getVisibility());
        }

        graph.flush();
    }
}
