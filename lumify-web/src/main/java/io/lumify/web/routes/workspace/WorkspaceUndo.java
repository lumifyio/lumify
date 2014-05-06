package io.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.detectedObjects.DetectedObjectModel;
import io.lumify.core.model.detectedObjects.DetectedObjectRepository;
import io.lumify.core.model.detectedObjects.DetectedObjectRowKey;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionModel;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.termMention.TermMentionRowKey;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.model.workspace.diff.SandboxStatus;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.LumifyVisibilityProperties;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import org.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.checkNotNull;

public class WorkspaceUndo extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceUndo.class);
    private final TermMentionRepository termMentionRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final UserRepository userRepository;
    private final WorkQueueRepository workQueueRepository;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public WorkspaceUndo(
            final TermMentionRepository termMentionRepository,
            final DetectedObjectRepository detectedObjectRepository,
            final Configuration configuration,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final UserRepository userRepository,
            final WorkspaceHelper workspaceHelper,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.termMentionRepository = termMentionRepository;
        this.detectedObjectRepository = detectedObjectRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceHelper = workspaceHelper;
        this.userRepository = userRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final JSONArray undoData = new JSONArray(getRequiredParameter(request, "undoData"));
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        JSONArray failures = new JSONArray();
        JSONArray successArray = new JSONArray();

        for (int i = 0; i < undoData.length(); i++) {
            JSONObject data = undoData.getJSONObject(i);
            String type = data.getString("type");
            if (type.equals("vertex")) {
                checkNotNull(data.getString("vertexId"));
                Vertex vertex = graph.getVertex(data.getString("vertexId"), authorizations);
                checkNotNull(vertex);
                if (data.getString("status").equals(SandboxStatus.PUBLIC.toString())) {
                    String msg = "Cannot undo a public vertex";
                    LOGGER.warn(msg);
                    data.put("error_msg", msg);
                    failures.put(data);
                    continue;
                }
                JSONObject responseResult = new JSONObject();
                responseResult.put("vertex", undoVertex(vertex, workspaceId, authorizations, user));
                successArray.put(responseResult);
            } else if (type.equals("relationship")) {
                Vertex sourceVertex = graph.getVertex(data.getString("sourceId"), authorizations);
                Vertex destVertex = graph.getVertex(data.getString("destId"), authorizations);
                if (sourceVertex == null || destVertex == null) {
                    continue;
                }
                Edge edge = graph.getEdge(data.getString("edgeId"), authorizations);
                checkNotNull(edge);
                if (data.getString("status").equals(SandboxStatus.PUBLIC.toString())) {
                    String error_msg = "Cannot undo a public edge";
                    LOGGER.warn(error_msg);
                    data.put("error_msg", error_msg);
                    failures.put(data);
                    continue;
                }
                JSONObject responseResult = new JSONObject();
                responseResult.put("edges", workspaceHelper.deleteEdge(edge, sourceVertex, destVertex, user, authorizations));
                successArray.put(responseResult);
            } else if (type.equals("property")) {
                checkNotNull(data.getString("vertexId"));
                Vertex vertex = graph.getVertex(data.getString("vertexId"), authorizations);
                if (vertex == null) {
                    continue;
                }
                if (data.getString("status").equals(SandboxStatus.PUBLIC.toString())) {
                    String error_msg = "Cannot undo a public property";
                    LOGGER.warn(error_msg);
                    data.put("error_msg", error_msg);
                    failures.put(data);
                    continue;
                }
                Property property = vertex.getProperty(data.getString("key"), data.getString("name"));
                JSONObject responseResult = new JSONObject();
                responseResult.put("property", workspaceHelper.deleteProperty(vertex, property, workspaceId, user));
                successArray.put(responseResult);
            }
        }

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", successArray);
        resultJson.put("failures", failures);
        respondWithJson(response, resultJson);
    }

    private JSONArray undoVertex(Vertex vertex, String workspaceId, Authorizations authorizations, User user) {
        JSONArray unresolved = new JSONArray();
        ModelUserContext modelUserContext = userRepository.getModelUserContext(authorizations, workspaceId, LumifyVisibility.SUPER_USER_VISIBILITY_STRING);
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(vertex);
        visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJson);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        for (Property rowKeyProperty : vertex.getProperties(LumifyProperties.ROW_KEY.getKey())) {
            TermMentionModel termMentionModel = termMentionRepository.findByRowKey(rowKeyProperty.getValue().toString(), userRepository.getModelUserContext(authorizations, LumifyVisibility.SUPER_USER_VISIBILITY_STRING));
            if (termMentionModel == null) {
                DetectedObjectModel detectedObjectModel = detectedObjectRepository.findByRowKey(rowKeyProperty.getValue().toString(), userRepository.getModelUserContext(authorizations, LumifyVisibility.SUPER_USER_VISIBILITY_STRING));
                if (detectedObjectModel == null) {
                    continue;
                } else {
                    DetectedObjectRowKey detectedObjectRowKey = new DetectedObjectRowKey(rowKeyProperty.getValue().toString());
                    DetectedObjectRowKey analyzedDetectedObjectRK = new DetectedObjectRowKey
                            (detectedObjectRowKey.getArtifactId(), detectedObjectModel.getMetadata().getX1(), detectedObjectModel.getMetadata().getY1(),
                                    detectedObjectModel.getMetadata().getX2(), detectedObjectModel.getMetadata().getY2());
                    DetectedObjectModel analyzedDetectedModel = detectedObjectRepository.findByRowKey(analyzedDetectedObjectRK.getRowKey(), modelUserContext);
                    JSONObject artifactVertexWithDetectedObjects = workspaceHelper.unresolveDetectedObject(vertex, detectedObjectModel.getMetadata().getEdgeId(), detectedObjectModel, analyzedDetectedModel, lumifyVisibility, workspaceId, modelUserContext, user, authorizations);
                    this.workQueueRepository.pushDetectedObjectChange(artifactVertexWithDetectedObjects);
                    unresolved.put(artifactVertexWithDetectedObjects);
                }
            } else {
                TermMentionRowKey termMentionRowKey = new TermMentionRowKey(rowKeyProperty.getValue().toString());
                unresolved.put(workspaceHelper.unresolveTerm(vertex, termMentionRowKey.getUniqueId(), termMentionModel, lumifyVisibility, modelUserContext, user, authorizations));
            }
        }

        Authorizations systemAuthorization = userRepository.getAuthorizations(user, WorkspaceRepository.VISIBILITY_STRING, workspaceId);
        Vertex workspaceVertex = graph.getVertex(workspaceId, systemAuthorization);
        for (Edge edge : workspaceVertex.getEdges(vertex, Direction.BOTH, systemAuthorization)) {
            graph.removeEdge(edge, systemAuthorization);
        }

        graph.removeVertex(vertex, authorizations);
        graph.flush();
        return unresolved;
    }
}
