package io.lumify.web.routes.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.*;

import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

@Singleton
public class WorkspaceHelper {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceHelper.class);
    private final TermMentionRepository termMentionRepository;
    private final AuditRepository auditRepository;
    private final WorkQueueRepository workQueueRepository;
    private final Graph graph;

    @Inject
    public WorkspaceHelper(
            final TermMentionRepository termMentionRepository,
            final AuditRepository auditRepository,
            final UserRepository userRepository,
            final WorkQueueRepository workQueueRepository,
            final Graph graph) {
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.workQueueRepository = workQueueRepository;
        this.graph = graph;
    }

    public void unresolveTerm(Vertex resolvedVertex, Vertex termMention, LumifyVisibility visibility, User user, String workspaceId, Authorizations authorizations) {
        Vertex sourceVertex = termMentionRepository.findSourceVertex(termMention, authorizations);
        if (sourceVertex == null) {
            return;
        }
        List<Edge> edges = toList(sourceVertex.getEdges(Direction.BOTH, authorizations));

        if (edges.size() == 1) {
            graph.removeEdge(edges.get(0), authorizations);
            workQueueRepository.pushEdgeDeletion(edges.get(0), workspaceId);
            auditRepository.auditRelationship(AuditAction.DELETE, sourceVertex, resolvedVertex, edges.get(0), "", "", user, visibility.getVisibility());
        }

        termMentionRepository.delete(termMention, authorizations);
        workQueueRepository.pushTextUpdated(sourceVertex.getId(), workspaceId);

        graph.flush();

        auditRepository.auditVertex(AuditAction.UNRESOLVE, resolvedVertex.getId(), "", "", user, visibility.getVisibility());
    }

    public void deleteProperty(Vertex vertex, Property property, String workspaceId, User user, Authorizations authorizations) {
        auditRepository.auditEntityProperty(AuditAction.DELETE, vertex.getId(), property.getKey(), property.getName(), property.getValue(), null, "", "", property.getMetadata(), user, property.getVisibility());

        vertex.removeProperty(property.getKey(), property.getName(), authorizations);

        graph.flush();

        workQueueRepository.pushGraphPropertyQueue(vertex, property, workspaceId);
    }

    public void deleteEdge(Edge edge, Vertex sourceVertex, Vertex destVertex, String imageRelationshipLabel, User user, String workspaceId, Authorizations authorizations) {
        graph.removeEdge(edge, authorizations);

        if (edge.getLabel().equals(imageRelationshipLabel)) {
            Property entityHasImage = sourceVertex.getProperty(LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName());
            if (entityHasImage != null) {
                sourceVertex.removeProperty(entityHasImage.getName(), authorizations);
                this.workQueueRepository.pushElementImageQueue(sourceVertex, entityHasImage, workspaceId);
            }
        }

        for (Vertex termMention : termMentionRepository.findByEdgeId(sourceVertex.getId(), edge.getId(), authorizations)) {
            termMentionRepository.delete(termMention, authorizations);
            workQueueRepository.pushTextUpdated(sourceVertex.getId(), workspaceId);
        }

        this.workQueueRepository.pushEdgeDeletion(edge, workspaceId);

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.DELETE, sourceVertex, destVertex, edge, "", "", user, new LumifyVisibility().getVisibility());

        graph.flush();
    }
}
