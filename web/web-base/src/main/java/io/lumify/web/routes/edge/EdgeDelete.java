package io.lumify.web.routes.edge;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.SandboxStatus;
import io.lumify.web.routes.workspace.WorkspaceHelper;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EdgeDelete extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(EdgeDelete.class);
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;
    private String entityHasImageIri;
    private final OntologyRepository ontologyRepository;

    @Inject
    public EdgeDelete(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final OntologyRepository ontologyRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.ontologyRepository = ontologyRepository;

        this.entityHasImageIri = ontologyRepository.getRelationshipIRIByIntent("entityHasImage");
        if (this.entityHasImageIri == null) {
            LOGGER.warn("'entityHasImage' intent has not been defined. Please update your ontology.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (this.entityHasImageIri == null) {
            this.entityHasImageIri = ontologyRepository.getRequiredRelationshipIRIByIntent("entityHasImage");
        }

        final String edgeId = getRequiredParameter(request, "edgeId");
        String workspaceId = getActiveWorkspaceId(request);

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Edge edge = graph.getEdge(edgeId, authorizations);
        Vertex sourceVertex = edge.getVertex(Direction.OUT, authorizations);
        Vertex destVertex = edge.getVertex(Direction.IN, authorizations);

        SandboxStatus sandboxStatus = GraphUtil.getSandboxStatus(edge, workspaceId);

        boolean isPublicEdge = sandboxStatus == SandboxStatus.PUBLIC;

        workspaceHelper.deleteEdge(workspaceId, edge, sourceVertex, destVertex, isPublicEdge, user, authorizations);
        respondWithSuccessJson(response);
    }
}
