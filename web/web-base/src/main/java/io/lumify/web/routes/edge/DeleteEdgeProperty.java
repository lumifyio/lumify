package io.lumify.web.routes.edge;

import io.lumify.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DeleteEdgeProperty extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public DeleteEdgeProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final AuditRepository auditRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        LumifyVisibility lumifyVisibility = new LumifyVisibility();
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String propertyKey = getRequiredParameter(request, "propertyKey");
        final String edgeId = getRequiredParameter(request, "edgeId");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyName);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        // TODO remove all properties from all edges? I don't think so
        Edge edge = graph.getEdge(edgeId, authorizations);
        Object oldValue = null;
        Property oldProperty = edge.getProperty(propertyKey, propertyName);
        if (oldProperty != null) {
            oldValue = oldProperty.getValue();
        }
        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationshipProperty(AuditAction.DELETE, edge.getVertexId(Direction.OUT), edge.getVertexId(Direction.IN), propertyKey, property.getTitle(),
                oldValue, null, edge, "", "", user, lumifyVisibility.getVisibility());
        edge.removeProperty(propertyKey, propertyName, authorizations);
        graph.flush();

        workQueueRepository.pushGraphPropertyQueue(edge, null, propertyName, workspaceId, null);

        respondWithSuccessJson(response);
    }
}
