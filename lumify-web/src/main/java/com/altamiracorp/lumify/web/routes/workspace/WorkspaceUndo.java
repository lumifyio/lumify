package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.diff.SandboxStatus;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;
import static org.elasticsearch.common.base.Preconditions.checkNotNull;

public class WorkspaceUndo extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceUndo.class);
    private final AuditRepository auditRepository;
    private final TermMentionRepository termMentionRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final Graph graph;
    private final ModelSession modelSession;
    private final VisibilityTranslator visibilityTranslator;
    private final UserProvider userProvider;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public WorkspaceUndo(final AuditRepository auditRepository,
                         final TermMentionRepository termMentionRepository,
                         final DetectedObjectRepository detectedObjectRepository,
                         final Configuration configuration,
                         final Graph graph,
                         final ModelSession modelSession,
                         final VisibilityTranslator visibilityTranslator,
                         final UserProvider userProvider,
                         final UserRepository userRepository,
                         final WorkspaceHelper workspaceHelper) {
        super(userRepository, configuration);
        this.auditRepository = auditRepository;
        this.termMentionRepository = termMentionRepository;
        this.detectedObjectRepository = detectedObjectRepository;
        this.graph = graph;
        this.modelSession = modelSession;
        this.visibilityTranslator = visibilityTranslator;
        this.userProvider = userProvider;
        this.workspaceHelper = workspaceHelper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final JSONArray undoData = new JSONArray(getRequiredParameter(request, "undoData"));
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getWorkspaceId(request);

        JSONArray failures = new JSONArray();
        JSONArray successArray = new JSONArray();

        /*
            [ { type: "vertex", vertexId:"vertexid", status: "PUBLIC..."},
            { type:"property", vertexId: "vertexid", key: "key", name: "name", status: "PUBLIC..."},
            { type:"relationship", edgeId:"edgeId", sourceId: "sourceId", destId: "destId", status: "PUBLIC..."}]
         */

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
                responseResult.put("edges", workspaceHelper.deleteEdge(edge, sourceVertex, destVertex, user, authorizations, null));
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
                responseResult.put("property", workspaceHelper.deleteProperty(vertex, toList(vertex.getProperties(data.getString("name"))), property, workspaceId));
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
        ModelUserContext modelUserContext = userProvider.getModelUserContext(authorizations, LumifyVisibility.VISIBILITY_STRING);
        String visibilityJsonString = (String) vertex.getPropertyValue(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), 0);
        JSONObject visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJsonString);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Iterator<Property> rowKeys = vertex.getProperties("_rowKey").iterator();
        while (rowKeys.hasNext()) {
            Property rowKeyProperty = rowKeys.next();
            TermMentionModel termMentionModel = termMentionRepository.findByRowKey((String) rowKeyProperty.getValue(), userProvider.getModelUserContext(authorizations, LumifyVisibility.VISIBILITY_STRING));
            if (termMentionModel == null) {
                DetectedObjectModel detectedObjectModel = detectedObjectRepository.findByRowKey((String) rowKeyProperty.getValue(), userProvider.getModelUserContext(authorizations, LumifyVisibility.VISIBILITY_STRING));
                if (detectedObjectModel == null) {
                    LOGGER.warn("No term mention or detected objects found for vertex, %s", vertex.getId());
                } else {
                    modelSession.alterAllColumnsVisibility(detectedObjectModel, lumifyVisibility.getVisibility().getVisibilityString(), FlushFlag.FLUSH);
                    unresolved.put(workspaceHelper.unresolveDetectedObject(vertex, detectedObjectModel, lumifyVisibility, workspaceId, modelUserContext, user, authorizations, null));
                }
            } else {
                modelSession.alterAllColumnsVisibility(termMentionModel, lumifyVisibility.getVisibility().getVisibilityString(), FlushFlag.FLUSH);
                unresolved.put(workspaceHelper.unresolveTerm(vertex, termMentionModel, lumifyVisibility, modelUserContext, user, authorizations, null));
            }
        }
        graph.removeVertex(vertex, authorizations);
        graph.flush();
        return unresolved;
    }
}
