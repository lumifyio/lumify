package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.model.textHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;

public class ResolveTermEntity extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ResolveTermEntity.class);
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public ResolveTermEntity(
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final VisibilityTranslator visibilityTranslator,
            final Configuration configuration,
            final TermMentionRepository termMentionRepository,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, configuration);
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
        this.workspaceRepository = workspaceRepository;
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

        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        Authorizations authorizations = getAuthorizations(request, user);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", STRINGS.getString("visibility.invalid"));
            chain.next(request, response);
            return;
        }

        Object id = graphVertexId == null ? graph.getIdGenerator().nextId() : graphVertexId;

        Concept concept = ontologyRepository.getConceptByIRI(conceptId);

        final Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        JSONObject visibilityJson = GraphUtil.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ElementMutation<Vertex> vertexMutation;
        Vertex vertex;
        if (graphVertexId != null) {
            vertex = graph.getVertex(id, authorizations);
            vertexMutation = vertex.prepareMutation();
        } else {
            vertexMutation = graph.prepareVertex(id, lumifyVisibility.getVisibility(), authorizations);
            GraphUtil.addJustificationToMutation(vertexMutation, justificationText, sourceInfo, lumifyVisibility);

            Map<String, Object> metadata = new HashMap<String, Object>();
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setMetadata(metadata, visibilityJson);

            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(vertexMutation, visibilityJson, lumifyVisibility.getVisibility());

            CONCEPT_TYPE.setProperty(vertexMutation, conceptId, metadata, lumifyVisibility.getVisibility());
            TITLE.setProperty(vertexMutation, title, metadata, lumifyVisibility.getVisibility());

            vertex = vertexMutation.save();

            auditRepository.auditVertexElementMutation(AuditAction.UPDATE, vertexMutation, vertex, "", user, lumifyVisibility.getVisibility());

            this.graph.flush();

//            workspaceRepository.updateEntityOnWorkspace(workspace, vertex.getId(), false, null, null, user);
        }


        // TODO: a better way to check if the same edge exists instead of looking it up every time?
        Edge edge = graph.addEdge(artifactVertex, vertex, LabelName.RAW_HAS_ENTITY.toString(), lumifyVisibility.getVisibility(), authorizations);
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(edge, visibilityJson, lumifyVisibility.getVisibility());

        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, vertex, edge, "", "", user, lumifyVisibility.getVisibility());
        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd, edge.getId().toString());
        TermMentionModel termMention = new TermMentionModel(termMentionRowKey);
        termMention.getMetadata()
                .setSign(title, lumifyVisibility.getVisibility())
                .setOntologyClassUri(concept.getDisplayName(), lumifyVisibility.getVisibility())
                .setConceptGraphVertexId(concept.getTitle(), lumifyVisibility.getVisibility())
                .setVertexId(vertex.getId().toString(), lumifyVisibility.getVisibility())
                .setEdgeId(edge.getId().toString(), lumifyVisibility.getVisibility());
        termMentionRepository.save(termMention);

        vertexMutation.addPropertyValue(graph.getIdGenerator().nextId().toString(), LumifyProperties.ROW_KEY.getKey(), termMentionRowKey.toString(), lumifyVisibility.getVisibility());
        vertexMutation.save();

        this.graph.flush();

        TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention);
        respondWithJson(response, offsetItem.toJson());
    }
}
