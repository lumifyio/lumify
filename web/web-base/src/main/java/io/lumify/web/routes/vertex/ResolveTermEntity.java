package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.SourceInfo;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResolveTermEntity extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ResolveTermEntity.class);
    private static final String MULTI_VALUE_KEY = ResolveTermEntity.class.getName();
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private String artifactHasEntityIri;

    @Inject
    public ResolveTermEntity(
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final VisibilityTranslator visibilityTranslator,
            final Configuration configuration,
            final TermMentionRepository termMentionRepository,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.artifactHasEntityIri = ontologyRepository.getRelationshipIRIByIntent("artifactHasEntity");
        if (this.artifactHasEntityIri == null) {
            LOGGER.warn("'artifactHasEntity' intent has not been defined. Please update your ontology.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (this.artifactHasEntityIri == null) {
            this.artifactHasEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactHasEntity");
        }

        final String artifactId = getRequiredParameter(request, "artifactId");
        final String propertyKey = getRequiredParameter(request, "propertyKey");
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String title = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String resolvedVertexId = getOptionalParameter(request, "resolvedVertexId");
        final String justificationText = getOptionalParameter(request, "justificationText");
        final String sourceInfoString = getOptionalParameter(request, "sourceInfo");

        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        Authorizations authorizations = getAuthorizations(request, user);

        VisibilityJson visibilityJson = GraphUtil.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility visibility = this.visibilityTranslator.toVisibility(visibilityJson);
        if (!graph.isVisibilityValid(visibility.getVisibility(), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        String id = resolvedVertexId == null ? graph.getIdGenerator().nextId() : resolvedVertexId;

        Concept concept = ontologyRepository.getConceptByIRI(conceptId);

        final Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Metadata metadata = new Metadata();
        LumifyProperties.VISIBILITY_JSON.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        ElementMutation<Vertex> vertexMutation;
        Vertex vertex;
        if (resolvedVertexId != null) {
            vertex = graph.getVertex(id, authorizations);
            vertexMutation = vertex.prepareMutation();
        } else {
            vertexMutation = graph.prepareVertex(id, lumifyVisibility.getVisibility());
            LumifyProperties.CONCEPT_TYPE.setProperty(vertexMutation, conceptId, metadata, lumifyVisibility.getVisibility());
            LumifyProperties.TITLE.addPropertyValue(vertexMutation, MULTI_VALUE_KEY, title, metadata, lumifyVisibility.getVisibility());
            vertex = vertexMutation.save(authorizations);

            SourceInfo sourceInfo = SourceInfo.fromString(sourceInfoString);
            GraphUtil.addJustification(graph, vertex, justificationText, sourceInfo, lumifyVisibility, authorizations);

            auditRepository.auditVertexElementMutation(AuditAction.UPDATE, vertexMutation, vertex, "", user, lumifyVisibility.getVisibility());

            LumifyProperties.VISIBILITY_JSON.setProperty(vertexMutation, visibilityJson, metadata, lumifyVisibility.getVisibility());

            this.graph.flush();

            workspaceRepository.updateEntityOnWorkspace(workspace, vertex.getId(), null, null, user);
        }

        // TODO: a better way to check if the same edge exists instead of looking it up every time?
        Edge edge = graph.addEdge(artifactVertex, vertex, this.artifactHasEntityIri, lumifyVisibility.getVisibility(), authorizations);
        LumifyProperties.VISIBILITY_JSON.setProperty(edge, visibilityJson, metadata, lumifyVisibility.getVisibility(), authorizations);

        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, vertex, edge, "", "", user, lumifyVisibility.getVisibility());

        new TermMentionBuilder()
                .sourceVertex(artifactVertex)
                .propertyKey(propertyKey)
                .start(mentionStart)
                .end(mentionEnd)
                .title(title)
                .conceptIri(concept.getIRI())
                .visibilityJson(visibilityJson)
                .resolvedTo(vertex, edge)
                .process(getClass().getSimpleName())
                .save(this.graph, visibilityTranslator, authorizations);

        vertexMutation.save(authorizations);

        this.graph.flush();
        workQueueRepository.pushTextUpdated(artifactId);

        workQueueRepository.pushElement(edge);

        respondWithSuccessJson(response);
    }
}
