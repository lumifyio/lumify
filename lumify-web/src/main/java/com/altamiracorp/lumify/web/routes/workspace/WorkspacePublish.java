package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.audit.Audit;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.diff.SandboxStatus;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.*;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.Messaging;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.elasticsearch.common.base.Preconditions.checkNotNull;

public class WorkspacePublish extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspacePublish.class);
    private final TermMentionRepository termMentionRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;
    private final OntologyRepository ontologyRepository;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public WorkspacePublish(final TermMentionRepository termMentionRepository,
                            final AuditRepository auditRepository,
                            final UserRepository userRepository,
                            final DetectedObjectRepository detectedObjectRepository,
                            final Configuration configuration,
                            final Graph graph,
                            final VisibilityTranslator visibilityTranslator,
                            final OntologyRepository ontologyRepository) {
        super(userRepository, configuration);
        this.detectedObjectRepository = detectedObjectRepository;
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.userRepository = userRepository;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final JSONArray publishData = new JSONArray(getRequiredParameter(request, "publishData"));
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        LOGGER.debug("publishing\n%s", publishData.toString(2));
        JSONArray failures = new JSONArray();
        publishVertices(publishData, failures, workspaceId, user, authorizations);
        publishEdges(publishData, failures, workspaceId, user, authorizations);
        publishProperties(publishData, failures, workspaceId, user, authorizations);

        JSONObject resultJson = new JSONObject();
        resultJson.put("failures", failures);
        resultJson.put("success", failures.length() == 0);
        LOGGER.debug("publishing results\n%s", resultJson.toString(2));
        respondWithJson(response, resultJson);
    }

    private void publishVertices(JSONArray publishData, JSONArray failures, String workspaceId, User user, Authorizations authorizations) {
        for (int i = 0; i < publishData.length(); i++) {
            JSONObject data = publishData.getJSONObject(i);
            try {
                String type = (String) data.get("type");
                String action = data.getString("action");
                if (!type.equals("vertex")) {
                    continue;
                }
                String vertexId = data.getString("vertexId");
                checkNotNull(vertexId);
                Vertex vertex = graph.getVertex(vertexId, authorizations);
                checkNotNull(vertex);
                if (data.getString("status").equals(SandboxStatus.PUBLIC.toString())) {
                    String msg;
                    if (action.equals("delete")) {
                        msg = "Cannot delete public vertex " + vertexId;
                    } else {
                        msg = "Vertex " + vertexId + " is already public";
                    }
                    LOGGER.warn(msg);
                    data.put("error_msg", msg);
                    failures.put(data);
                    continue;
                }
                publishVertex(vertex, action, authorizations, workspaceId, user);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(2), ex);
                data.put("error_msg", ex.getMessage());
                failures.put(data);
            }
        }
        graph.flush();
    }

    private void publishEdges(JSONArray publishData, JSONArray failures, String workspaceId, User user, Authorizations authorizations) {
        for (int i = 0; i < publishData.length(); i++) {
            JSONObject data = publishData.getJSONObject(i);
            try {
                String type = (String) data.get("type");
                String action = data.getString("action");
                if (!type.equals("relationship")) {
                    continue;
                }
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
                    continue;
                }

                if (sourceVertex != null && destVertex != null && GraphUtil.getSandboxStatus(sourceVertex, workspaceId) != SandboxStatus.PUBLIC &&
                        GraphUtil.getSandboxStatus(destVertex, workspaceId) != SandboxStatus.PUBLIC) {
                    String error_msg = "Cannot publish edge, " + edge.getId().toString() + ", because either source and/or dest vertex are not public";
                    LOGGER.warn(error_msg);
                    data.put("error_msg", error_msg);
                    failures.put(data);
                    continue;
                }
                publishEdge(edge, sourceVertex, destVertex, action, workspaceId, user, authorizations);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(2), ex);
                data.put("error_msg", ex.getMessage());
                failures.put(data);
            }
        }
        graph.flush();
    }

    private void publishProperties(JSONArray publishData, JSONArray failures, String workspaceId, User user, Authorizations authorizations) {
        for (int i = 0; i < publishData.length(); i++) {
            JSONObject data = publishData.getJSONObject(i);
            try {
                String type = (String) data.get("type");
                String action = data.getString("action");
                if (!type.equals("property")) {
                    continue;
                }
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
                    continue;
                }

                if (GraphUtil.getSandboxStatus(vertex, workspaceId) != SandboxStatus.PUBLIC) {
                    String error_msg = "Cannot publish a modification of a property on a private vertex: " + vertex.getId().toString();
                    LOGGER.warn(error_msg);
                    data.put("error_msg", error_msg);
                    failures.put(data);
                    continue;
                }

                publishProperty(vertex, action, data.getString("key"), data.getString("name"), workspaceId, user);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(2), ex);
                data.put("error_msg", ex.getMessage());
                failures.put(data);
            }
        }
        graph.flush();
    }

    private void publishVertex(Vertex vertex, String action, Authorizations authorizations, String workspaceId, User user) throws IOException {
        if (action.equals("delete")) {
            graph.removeVertex(vertex, authorizations);
            return;
        }

        LOGGER.debug("publishing vertex %s(%s)", vertex.getId().toString(), vertex.getVisibility().toString());
        String originalVertexVisibility = vertex.getVisibility().getVisibilityString();
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(vertex);
        JSONArray workspaceJsonArray = JSONUtil.getOrCreateJSONArray(visibilityJson, VisibilityTranslator.JSON_WORKSPACES);
        if (!JSONUtil.arrayContains(workspaceJsonArray, workspaceId)) {
            throw new LumifyException(String.format("vertex with id '%s' is not local to workspace '%s'", vertex.getId(), workspaceId));
        }

        visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJson);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ExistingElementMutation<Vertex> vertexElementMutation = vertex.prepareMutation();
        vertex.removeProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getKey());
        vertexElementMutation.alterElementVisibility(lumifyVisibility.getVisibility());

        for (Property property : vertex.getProperties()) {
            OntologyProperty ontologyProperty = ontologyRepository.getProperty(property.getName());
            checkNotNull(ontologyProperty, "Could not find property " + property.getName());
            if (!ontologyProperty.getUserVisible()) {
                publishProperty(vertexElementMutation, property, workspaceId, user);
            }
        }

        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(vertexElementMutation, visibilityJson, lumifyVisibility.getVisibility());
        vertexElementMutation.save();

        ModelUserContext systemModelUser = userRepository.getModelUserContext(authorizations, LumifyVisibility.SUPER_USER_VISIBILITY_STRING);

        for (Audit row : auditRepository.findByRowStartsWith(vertex.getId().toString(), systemModelUser)) {
            auditRepository.updateColumnVisibility(row, originalVertexVisibility, lumifyVisibility.getVisibility().getVisibilityString(), FlushFlag.FLUSH);
        }
    }

    private void publishProperty(Element element, String action, String key, String name, String workspaceId, User user) {
        if (action.equals("delete")) {
            element.removeProperty(key, name);
            return;
        }
        ExistingElementMutation elementMutation = element.prepareMutation();
        Iterable<Property> properties = element.getProperties(name);
        for (Property property : properties) {
            if (!property.getKey().equals(key)) {
                continue;
            }
            if (publishProperty(elementMutation, property, workspaceId, user)) {
                elementMutation.save();
                return;
            }
        }
        throw new LumifyException(String.format("no property with key '%s' and name '%s' found on workspace '%s'", key, name, workspaceId));
    }

    private boolean publishProperty(ExistingElementMutation elementMutation, Property property, String workspaceId, User user) {
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getMetadataValue(property.getMetadata());
        if (visibilityJson == null) {
            LOGGER.debug("skipping property %s. no visibility json property", property.toString());
            return false;
        }
        JSONArray workspaceJsonArray = JSONUtil.getOrCreateJSONArray(visibilityJson, VisibilityTranslator.JSON_WORKSPACES);
        if (!JSONUtil.arrayContains(workspaceJsonArray, workspaceId)) {
            LOGGER.debug("skipping property %s. doesn't have workspace in json.", property.toString());
            return false;
        }

        LOGGER.debug("publishing property %s:%s(%s)", property.getKey(), property.getName(), property.getVisibility().toString());
        visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJson);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        elementMutation
                .alterPropertyVisibility(property, lumifyVisibility.getVisibility())
                .alterPropertyMetadata(property, LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getKey(), visibilityJson.toString());

        auditRepository.auditEntityProperty(AuditAction.PUBLISH, elementMutation.getElement().getId(), property.getName(), property.getValue(), property.getValue(), "", "", property.getMetadata(), user, lumifyVisibility.getVisibility());
        return true;
    }

    private void publishEdge(Edge edge, Vertex sourceVertex, Vertex destVertex, String action, String workspaceId, User user, Authorizations authorizations) {
        if (action.equals("delete")) {
            graph.removeEdge(edge, authorizations);
            return;
        }

        LOGGER.debug("publishing edge %s(%s)", edge.getId().toString(), edge.getVisibility().toString());
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(edge);
        JSONArray workspaceJsonArray = JSONUtil.getOrCreateJSONArray(visibilityJson, VisibilityTranslator.JSON_WORKSPACES);
        if (!JSONUtil.arrayContains(workspaceJsonArray, workspaceId)) {
            throw new LumifyException(String.format("edge with id '%s' is not local to workspace '%s'", edge.getId(), workspaceId));
        }

        if (edge.getLabel().equals(LabelName.ENTITY_HAS_IMAGE_RAW.toString())) {
            publishGlyphIconProperty(edge, workspaceId, user, authorizations);
        }

        edge.removeProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getKey());
        visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJson);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ExistingElementMutation<Edge> edgeExistingElementMutation = edge.prepareMutation();
        String originalEdgeVisibility = edge.getVisibility().getVisibilityString();
        edgeExistingElementMutation.alterElementVisibility(lumifyVisibility.getVisibility());

        for (Property property : edge.getProperties()) {
            publishProperty(edgeExistingElementMutation, property, workspaceId, user);
        }

        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(edgeExistingElementMutation, visibilityJson, lumifyVisibility.getVisibility());
        auditRepository.auditEdgeElementMutation(AuditAction.PUBLISH, edgeExistingElementMutation, edge, sourceVertex, destVertex, "", user, lumifyVisibility.getVisibility());
        edge = edgeExistingElementMutation.save();

        auditRepository.auditRelationship(AuditAction.PUBLISH, sourceVertex, destVertex, edge, "", "", user, edge.getVisibility());

        ModelUserContext systemUser = userRepository.getModelUserContext(authorizations, LumifyVisibility.SUPER_USER_VISIBILITY_STRING);
        for (Audit row : auditRepository.findByRowStartsWith(edge.getId().toString(), systemUser)) {
            auditRepository.updateColumnVisibility(row, originalEdgeVisibility, lumifyVisibility.getVisibility().getVisibilityString(), FlushFlag.FLUSH);
        }

        for (Property rowKeyProperty : destVertex.getProperties(LumifyProperties.ROW_KEY.getKey())) {
            TermMentionModel termMentionModel = termMentionRepository.findByRowKey((String) rowKeyProperty.getValue(), systemUser);
            if (termMentionModel == null) {
                DetectedObjectModel detectedObjectModel = detectedObjectRepository.findByRowKey((String) rowKeyProperty.getValue(), systemUser);
                if (detectedObjectModel == null) {
                    LOGGER.warn("No term mention or detected objects found for vertex, %s", sourceVertex.getId());
                } else {
                    detectedObjectRepository.updateColumnVisibility(detectedObjectModel, originalEdgeVisibility, lumifyVisibility.getVisibility().getVisibilityString(), FlushFlag.FLUSH);

                    Vertex artifactVertex = graph.getVertex(detectedObjectModel.getRowKey().getArtifactId(), authorizations);
                    JSONObject artifactVertexWithDetectedObjects = JsonSerializer.toJsonVertex(artifactVertex, workspaceId);
                    artifactVertexWithDetectedObjects.put("detectedObjects", detectedObjectRepository.toJSON(artifactVertex, systemUser, authorizations, workspaceId));

                    Messaging.broadcastDetectedObjectChange(artifactVertex.getId().toString(), artifactVertexWithDetectedObjects);
                }
            } else {
                termMentionRepository.updateColumnVisibility(termMentionModel, originalEdgeVisibility, lumifyVisibility.getVisibility().getVisibilityString(), FlushFlag.FLUSH);
            }
        }
    }

    private void publishGlyphIconProperty(Edge hasImageEdge, String workspaceId, User user, Authorizations authorizations) {
        Vertex entityVertex = hasImageEdge.getVertex(Direction.OUT, authorizations);
        checkNotNull(entityVertex, "Could not find has image source vertex " + hasImageEdge.getVertexId(Direction.OUT));
        ExistingElementMutation elementMutation = entityVertex.prepareMutation();
        Iterable<Property> glyphIconProperties = entityVertex.getProperties(LumifyProperties.GLYPH_ICON.getKey());
        for (Property glyphIconProperty : glyphIconProperties) {
            if (publishProperty(elementMutation, glyphIconProperty, workspaceId, user)) {
                elementMutation.save();
                return;
            }
        }
        LOGGER.warn("new has image edge without a glyph icon property being set on vertex %s", entityVertex.getId());
    }
}
