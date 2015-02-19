package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.ingest.ArtifactDetectedObject;
import io.lumify.core.model.SourceInfo;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiElement;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResolveDetectedObject extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ResolveDetectedObject.class);
    private static final String MULTI_VALUE_KEY_PREFIX = ResolveDetectedObject.class.getName();
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final TermMentionRepository termMentionRepository;
    private String artifactContainsImageOfEntityIri;

    @Inject
    public ResolveDetectedObject(
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkQueueRepository workQueueRepository,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository,
            final TermMentionRepository termMentionRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.termMentionRepository = termMentionRepository;

        this.artifactContainsImageOfEntityIri = ontologyRepository.getRelationshipIRIByIntent("artifactContainsImageOfEntity");
        if (this.artifactContainsImageOfEntityIri == null) {
            LOGGER.warn("'artifactContainsImageOfEntity' intent has not been defined. Please update your ontology.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (this.artifactContainsImageOfEntityIri == null) {
            this.artifactContainsImageOfEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactContainsImageOfEntity");
        }

        final String artifactId = getRequiredParameter(request, "artifactId");
        final String title = getRequiredParameter(request, "title");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String graphVertexId = getOptionalParameter(request, "graphVertexId");
        final String justificationText = getOptionalParameter(request, "justificationText");
        final String sourceInfoString = getOptionalParameter(request, "sourceInfo");
        String originalPropertyKey = getOptionalParameter(request, "originalPropertyKey");
        double x1 = Double.parseDouble(getRequiredParameter(request, "x1"));
        double x2 = Double.parseDouble(getRequiredParameter(request, "x2"));
        double y1 = Double.parseDouble(getRequiredParameter(request, "y1"));
        double y2 = Double.parseDouble(getRequiredParameter(request, "y2"));

        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        Authorizations authorizations = getAuthorizations(request, user);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        VisibilityJson visibilityJson = GraphUtil.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        Concept concept = ontologyRepository.getConceptByIRI(conceptId);
        Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        ElementMutation<Vertex> resolvedVertexMutation;

        Metadata metadata = new Metadata();
        LumifyProperties.VISIBILITY_JSON.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());

        Vertex resolvedVertex;
        if (graphVertexId == null || graphVertexId.equals("")) {
            resolvedVertexMutation = graph.prepareVertex(lumifyVisibility.getVisibility());

            LumifyProperties.CONCEPT_TYPE.setProperty(resolvedVertexMutation, concept.getIRI(), metadata, lumifyVisibility.getVisibility());
            LumifyProperties.TITLE.setProperty(resolvedVertexMutation, title, metadata, lumifyVisibility.getVisibility());

            resolvedVertex = resolvedVertexMutation.save(authorizations);
            auditRepository.auditVertexElementMutation(AuditAction.UPDATE, resolvedVertexMutation, resolvedVertex, "", user, lumifyVisibility.getVisibility());
            SourceInfo sourceInfo = SourceInfo.fromString(sourceInfoString);
            termMentionRepository.addJustification(resolvedVertex, justificationText, sourceInfo, lumifyVisibility, authorizations);

            resolvedVertex = resolvedVertexMutation.save(authorizations);

            auditRepository.auditVertexElementMutation(AuditAction.UPDATE, resolvedVertexMutation, resolvedVertex, "", user, lumifyVisibility.getVisibility());
            LumifyProperties.VISIBILITY_JSON.setProperty(resolvedVertexMutation, visibilityJson, metadata, lumifyVisibility.getVisibility());

            graph.flush();

            workspaceRepository.updateEntityOnWorkspace(workspace, resolvedVertex.getId(), null, null, user);
        } else {
            resolvedVertex = graph.getVertex(graphVertexId, authorizations);
            resolvedVertexMutation = resolvedVertex.prepareMutation();
        }

        Edge edge = graph.addEdge(artifactVertex, resolvedVertex, artifactContainsImageOfEntityIri, lumifyVisibility.getVisibility(), authorizations);
        LumifyProperties.VISIBILITY_JSON.setProperty(edge, visibilityJson, metadata, lumifyVisibility.getVisibility(), authorizations);
        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, resolvedVertex, edge, "", "", user, lumifyVisibility.getVisibility());

        ArtifactDetectedObject artifactDetectedObject = new ArtifactDetectedObject(
                x1,
                y1,
                x2,
                y2,
                concept.getIRI(),
                "user",
                edge.getId(),
                resolvedVertex.getId(),
                originalPropertyKey);
        String propertyKey = artifactDetectedObject.getMultivalueKey(MULTI_VALUE_KEY_PREFIX);
        LumifyProperties.DETECTED_OBJECT.addPropertyValue(artifactVertex, propertyKey, artifactDetectedObject, lumifyVisibility.getVisibility(), authorizations);

        resolvedVertexMutation.addPropertyValue(resolvedVertex.getId(), LumifyProperties.ROW_KEY.getPropertyName(), propertyKey, lumifyVisibility.getVisibility());
        resolvedVertexMutation.save(authorizations);

        graph.flush();

        workQueueRepository.pushElement(edge);
        workQueueRepository.pushGraphPropertyQueue(artifactVertex, propertyKey, LumifyProperties.DETECTED_OBJECT.getPropertyName());

        ClientApiElement result = ClientApiConverter.toClientApi(artifactVertex, workspaceId, authorizations);
        respondWithClientApiObject(response, result);
    }
}
