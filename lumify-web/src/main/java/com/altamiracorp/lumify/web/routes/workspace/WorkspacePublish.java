package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
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
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;

import static org.elasticsearch.common.base.Preconditions.checkNotNull;

public class WorkspacePublish extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspacePublish.class);
    private final TermMentionRepository termMentionRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final AuditRepository auditRepository;
    private final UserProvider userProvider;
    private final ModelSession modelSession;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public WorkspacePublish(final TermMentionRepository termMentionRepository,
                            final AuditRepository auditRepository,
                            final UserRepository userRepository,
                            final DetectedObjectRepository detectedObjectRepository,
                            final Configuration configuration,
                            final Graph graph,
                            final ModelSession modelSession,
                            final VisibilityTranslator visibilityTranslator,
                            final UserProvider userProvider) {
        super(userRepository, configuration);
        this.detectedObjectRepository = detectedObjectRepository;
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.modelSession = modelSession;
        this.userProvider = userProvider;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final JSONArray publishData = new JSONArray(getRequiredParameter(request, "publishData"));
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getWorkspaceId(request);

        JSONArray failures = new JSONArray();
        boolean success = false;
        for (int i = 0; i < publishData.length(); i++) {
            JSONObject data = publishData.getJSONObject(i);
            String type = (String) data.get("type");
            String action = data.getString("action");
            if (type.equals("vertex")) {
                checkNotNull(data.getString("vertexId"));
                Vertex vertex = graph.getVertex(data.getString("vertexId"), authorizations);
                checkNotNull(vertex);
                if (data.getString("status").equals(SandboxStatus.PUBLIC.toString())) {
                    String msg;
                    if (action.equals("delete")) {
                        msg = "Cannot delete a public vertex";
                    } else {
                        msg = "Vertex is already public";
                    }
                    LOGGER.warn(msg);
                    data.put("error_msg", msg);
                    failures.put(data);
                    publishData.remove(i);
                    continue;
                }
                publishVertex(vertex, action, authorizations, user);
                success = true;
                publishData.remove(i);
            }
        }
        graph.flush();

        for (int i = 0; i < publishData.length(); i++) {
            JSONObject data = publishData.getJSONObject(i);
            String type = (String) data.get("type");
            String action = data.getString("action");
            if (type.equals("relationship")) {
                Edge edge = graph.getEdge(data.getString("edgeId"), authorizations);
                Vertex sourceVertex = graph.getVertex(data.getString("sourceId"), authorizations);
                Vertex destVertex = graph.getVertex(data.getString("destId"), authorizations);
                if (data.getString("status").equals(SandboxStatus.PUBLIC.toString())) {
                    String error_msg;
                    if (action.equals("delete")) {
                        error_msg = "Cannot delete a public edge";
                    } else {
                        error_msg = "Edge is already public";
                    }
                    LOGGER.warn(error_msg);
                    data.put("error_msg", error_msg);
                    failures.put(data);
                    publishData.remove(i);
                    continue;
                }

                if (sourceVertex != null && destVertex != null && GraphUtil.getSandboxStatus(sourceVertex, workspaceId) != SandboxStatus.PUBLIC &&
                        GraphUtil.getSandboxStatus(destVertex, workspaceId) != SandboxStatus.PUBLIC) {
                    String error_msg = "Cannot publish edge, " + edge.getId().toString() + ", because either source and/or dest vertex are not public";
                    LOGGER.warn(error_msg);
                    data.put("error_msg", error_msg);
                    failures.put(data);
                    publishData.remove(i);
                    continue;
                }
                publishEdge(edge, action, authorizations);
                success = true;
                publishData.remove(i);
            }
        }
        graph.flush();

        for (int i = 0; i < publishData.length(); i++) {
            JSONObject data = publishData.getJSONObject(i);
            String type = (String) data.get("type");
            String action = data.getString("action");
            if (type.equals("property")) {
                checkNotNull(data.getString("vertexId"));
                Vertex vertex = graph.getVertex(data.getString("vertexId"), authorizations);
                checkNotNull(vertex);
                if (data.getString("status").equals(SandboxStatus.PUBLIC.toString())) {
                    String error_msg;
                    if (action.equals("delete")) {
                        error_msg = "Cannot delete a public property";
                    } else {
                        error_msg = "Property is already public";
                    }
                    LOGGER.warn(error_msg);
                    data.put("error_msg", error_msg);
                    failures.put(data);
                    publishData.remove(i);
                    continue;
                }

                if (GraphUtil.getSandboxStatus(vertex, workspaceId) != SandboxStatus.PUBLIC) {
                    String error_msg = "Cannot publish a modification of a property on a private vertex: " + vertex.getId().toString();
                    LOGGER.warn(error_msg);
                    data.put("error_msg", error_msg);
                    failures.put(data);
                    publishData.remove(i);
                    continue;
                }

                publishProperty(vertex, action, data.getString("key"), data.getString("name"));
                success = true;
            } else {
                throw new RuntimeException(type + " type is not supported for publishing");
            }
        }
        JSONObject resultJson = new JSONObject();
        resultJson.put("failures", failures);
        resultJson.put("success", success);
        respondWithJson(response, resultJson);
    }

    private void publishVertex(Vertex vertex, String action, Authorizations authorizations, User user) throws IOException {
        if (action.equals("delete")) {
            graph.removeVertex(vertex, authorizations);
            return;
        }
        String visibilityJsonString = (String) vertex.getPropertyValue(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), 0);
        JSONObject visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJsonString);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ExistingElementMutation<Vertex> vertexElementMutation = vertex.prepareMutation();
        vertex.removeProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString());
        Iterator properties = vertex.getProperties().iterator();
        vertexElementMutation.alterElementVisibility(lumifyVisibility.getVisibility());
        while (properties.hasNext()) {
            Property property = (Property) properties.next();
            if (property.getName().contains("_") || property.getName().equals("title")) {
                vertexElementMutation.alterPropertyVisibility(property.getKey(), property.getName(), lumifyVisibility.getVisibility());
            }
        }
        vertexElementMutation.setProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), visibilityJson.toString(), lumifyVisibility.getVisibility());
        vertexElementMutation.save();

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
                }
            } else {
                modelSession.alterAllColumnsVisibility(termMentionModel, lumifyVisibility.getVisibility().getVisibilityString(), FlushFlag.FLUSH);
            }
        }
        vertex.removeProperty("_rowKey");
    }

    private void publishProperty(Vertex vertex, String action, String key, String name) {
        if (action.equals("delete")) {
            vertex.removeProperty(key, name);
            return;
        }
        Property property = vertex.getProperty(key, name);
        String visibilityJsonString = (String) property.getMetadata().get(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString());
        JSONObject visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJsonString);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        vertex.prepareMutation()
                .alterPropertyVisibility(property.getKey(), property.getName(), lumifyVisibility.getVisibility())
                .alterPropertyMetadata(property.getKey(), property.getName(), LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), visibilityJson.toString())
                .save();
    }

    private void publishEdge(Edge edge, String action, Authorizations authorizations) {
        if (action.equals("delete")) {
            graph.removeEdge(edge, authorizations);
            return;
        }
        String visibilityJsonString = (String) edge.getPropertyValue(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), 0);
        edge.removeProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString());
        JSONObject visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJsonString);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ExistingElementMutation<Edge> edgeExistingElementMutation = edge.prepareMutation();
        Iterator properties = edge.getProperties().iterator();
        edgeExistingElementMutation.alterElementVisibility(lumifyVisibility.getVisibility());
        while (properties.hasNext()) {
            Property property = (Property) properties.next();
            if (property.getName().contains("_") || property.getName().equals("title")) {
                edgeExistingElementMutation
                        .alterPropertyVisibility(property.getKey(), property.getName(), lumifyVisibility.getVisibility());
            }
        }
        edgeExistingElementMutation.setProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), visibilityJson.toString(), lumifyVisibility.getVisibility());
        edgeExistingElementMutation.save();
    }
}
