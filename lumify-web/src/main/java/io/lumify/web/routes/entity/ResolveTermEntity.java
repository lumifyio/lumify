package io.lumify.web.routes.entity;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionModel;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.termMention.TermMentionRowKey;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.LumifyVisibilityProperties;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.mutation.ElementMutation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static io.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static io.lumify.core.model.properties.LumifyProperties.TITLE;

public class ResolveTermEntity extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ResolveTermEntity.class);
    private static final String MULTI_VALUE_KEY = ResolveTermEntity.class.getName();
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final String artifactHasEntityIri;

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
        this.termMentionRepository = termMentionRepository;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;

        this.artifactHasEntityIri = this.getConfiguration().get(Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY);
        if (this.artifactHasEntityIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String artifactId = getRequiredParameter(request, "artifactId");
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String title = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String graphVertexId = getOptionalParameter(request, "graphVertexId");
        final String justificationText = getOptionalParameter(request, "justificationText");
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");
        final String rowKey = getOptionalParameter(request, "rowKey");

        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        Authorizations authorizations = getAuthorizations(request, user);

        JSONObject visibilityJson = GraphUtil.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility visibility = this.visibilityTranslator.toVisibility(visibilityJson);
        if (!graph.isVisibilityValid(visibility.getVisibility(), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", STRINGS.getString("visibility.invalid"));
            chain.next(request, response);
            return;
        }

        Object id = graphVertexId == null ? graph.getIdGenerator().nextId() : graphVertexId;

        Concept concept = ontologyRepository.getConceptByIRI(conceptId);

        final Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Map<String, Object> metadata = new HashMap<String, Object>();
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setMetadata(metadata, visibilityJson);
        ElementMutation<Vertex> vertexMutation;
        Vertex vertex;
        if (graphVertexId != null) {
            vertex = graph.getVertex(id, authorizations);
            vertexMutation = vertex.prepareMutation();
        } else {
            vertexMutation = graph.prepareVertex(id, lumifyVisibility.getVisibility());
            GraphUtil.addJustificationToMutation(vertexMutation, justificationText, sourceInfo, lumifyVisibility);

            CONCEPT_TYPE.setProperty(vertexMutation, conceptId, metadata, lumifyVisibility.getVisibility());
            TITLE.addPropertyValue(vertexMutation, MULTI_VALUE_KEY, title, metadata, lumifyVisibility.getVisibility());

            vertex = vertexMutation.save(authorizations);

            auditRepository.auditVertexElementMutation(AuditAction.UPDATE, vertexMutation, vertex, "", user, lumifyVisibility.getVisibility());

            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(vertexMutation, visibilityJson, metadata, lumifyVisibility.getVisibility());

            this.graph.flush();

            workspaceRepository.updateEntityOnWorkspace(workspace, vertex.getId(), false, null, null, user);
        }

        // TODO: a better way to check if the same edge exists instead of looking it up every time?
        Edge edge = graph.addEdge(artifactVertex, vertex, this.artifactHasEntityIri, lumifyVisibility.getVisibility(), authorizations);
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(edge, visibilityJson, metadata, lumifyVisibility.getVisibility(), authorizations);

        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, vertex, edge, "", "", user, lumifyVisibility.getVisibility());

        String propertyKey = "";
        if (rowKey != null) {
            TermMentionRowKey analyzedRowKey = new TermMentionRowKey(rowKey);
            propertyKey = analyzedRowKey.getPropertyKey();
        }

        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, propertyKey, mentionStart, mentionEnd, edge.getId().toString());
        TermMentionModel termMention = new TermMentionModel(termMentionRowKey);
        termMention.getMetadata()
                .setSign(title, lumifyVisibility.getVisibility())
                .setOntologyClassUri(concept.getDisplayName(), lumifyVisibility.getVisibility())
                .setConceptGraphVertexId(concept.getTitle(), lumifyVisibility.getVisibility())
                .setVertexId(vertex.getId().toString(), lumifyVisibility.getVisibility())
                .setEdgeId(edge.getId().toString(), lumifyVisibility.getVisibility());
        termMentionRepository.save(termMention);

        vertexMutation.addPropertyValue(graph.getIdGenerator().nextId().toString(), LumifyProperties.ROW_KEY.getKey(), termMentionRowKey.toString(), metadata, lumifyVisibility.getVisibility());
        vertexMutation.save(authorizations);

        this.graph.flush();
        workQueueRepository.pushTextUpdated(artifactId);

        workQueueRepository.pushElement(edge);

        JSONObject result = new JSONObject();
        result.put("success", true);
        respondWithJson(response, result);
    }
}
