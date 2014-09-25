package io.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.audit.Audit;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.model.workspace.diff.SandboxStatus;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.JSONUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.mutation.ExistingElementMutation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;

public class WorkspacePublish extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspacePublish.class);
    private final TermMentionRepository termMentionRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;
    private final OntologyRepository ontologyRepository;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final String entityHasImageIri;

    @Inject
    public WorkspacePublish(
            final TermMentionRepository termMentionRepository,
            final AuditRepository auditRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final OntologyRepository ontologyRepository,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.userRepository = userRepository;
        this.ontologyRepository = ontologyRepository;

        this.entityHasImageIri = this.getConfiguration().get(Configuration.ONTOLOGY_IRI_ENTITY_HAS_IMAGE);
        if (this.entityHasImageIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ENTITY_HAS_IMAGE);
        }
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
        LOGGER.debug("BEGIN publishVertices");
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
                if (GraphUtil.getSandboxStatus(vertex, workspaceId) == SandboxStatus.PUBLIC) {
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
        LOGGER.debug("END publishVertices");
        graph.flush();
    }

    private void publishEdges(JSONArray publishData, JSONArray failures, String workspaceId, User user, Authorizations authorizations) {
        LOGGER.debug("BEGIN publishEdges");
        for (int i = 0; i < publishData.length(); i++) {
            JSONObject data = publishData.getJSONObject(i);
            try {
                String type = (String) data.get("type");
                String action = data.getString("action");
                if (!type.equals("relationship")) {
                    continue;
                }
                Edge edge = graph.getEdge(data.getString("edgeId"), authorizations);
                Vertex sourceVertex = edge.getVertex(Direction.OUT, authorizations);
                Vertex destVertex = edge.getVertex(Direction.IN, authorizations);
                if (GraphUtil.getSandboxStatus(edge, workspaceId) == SandboxStatus.PUBLIC) {
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
                    String error_msg = "Cannot publish edge, " + edge.getId() + ", because either source and/or dest vertex are not public";
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
        LOGGER.debug("END publishEdges");
        graph.flush();
    }

    private void publishProperties(JSONArray publishData, JSONArray failures, String workspaceId, User user, Authorizations authorizations) {
        LOGGER.debug("BEGIN publishProperties");
        for (int i = 0; i < publishData.length(); i++) {
            JSONObject data = publishData.getJSONObject(i);
            try {
                String type = (String) data.get("type");
                String action = data.getString("action");
                if (!type.equals("property")) {
                    continue;
                }
                Element element = getPropertyElement(authorizations, data);

                String propertyKey = data.getString("key");
                String propertyName = data.getString("name");

                OntologyProperty ontologyProperty = ontologyRepository.getProperty(propertyName);
                if (!ontologyProperty.getUserVisible() || propertyName.equals(LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName())) {
                    continue;
                }

                List<Property> properties = toList(element.getProperties(propertyName));
                SandboxStatus[] sandboxStatuses = GraphUtil.getPropertySandboxStatuses(properties, workspaceId);
                boolean propertyFailed = false;
                for (int propertyIndex = 0; propertyIndex < properties.size(); propertyIndex++) {
                    Property property = properties.get(propertyIndex);
                    if (!property.getKey().equals(propertyKey)) {
                        continue;
                    }
                    SandboxStatus propertySandboxStatus = sandboxStatuses[propertyIndex];

                    if (propertySandboxStatus == SandboxStatus.PUBLIC) {
                        String error_msg;
                        if (action.equals("delete")) {
                            error_msg = "Cannot delete a public property";
                        } else {
                            error_msg = "Property is already public";
                        }
                        LOGGER.warn(error_msg);
                        data.put("error_msg", error_msg);
                        failures.put(data);
                        propertyFailed = true;
                    }
                }

                if (propertyFailed) {
                    continue;
                }

                if (GraphUtil.getSandboxStatus(element, workspaceId) != SandboxStatus.PUBLIC) {
                    String errorMessage = "Cannot publish a modification of a property on a private element: " + element.getId();
                    JSONObject visibilityJson = LumifyProperties.VISIBILITY_SOURCE.getPropertyValue(element);
                    LOGGER.warn("%s: visibilityJson: %s, workspaceId: %s", errorMessage, visibilityJson.toString(), workspaceId);
                    data.put("error_msg", errorMessage);
                    failures.put(data);
                    continue;
                }

                publishProperty(element, action, propertyKey, propertyName, workspaceId, user, authorizations);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(2), ex);
                data.put("error_msg", ex.getMessage());
                failures.put(data);
            }
        }
        LOGGER.debug("END publishProperties");
        graph.flush();
    }

    private Element getPropertyElement(Authorizations authorizations, JSONObject data) {
        Element element = null;

        String elementId = data.optString("edgeId", null);
        if (elementId != null) {
            element = graph.getEdge(elementId, authorizations);
        }

        if (element == null) {
            elementId = data.optString("vertexId", null);
            if (elementId != null) {
                element = graph.getVertex(elementId, authorizations);
            }
        }

        if (element == null) {
            elementId = data.optString("elementId", null);
            checkNotNull(elementId, "elementId, vertexId, or edgeId is required to publish a property");
            element = graph.getVertex(elementId, authorizations);
            if (element == null) {
                element = graph.getEdge(elementId, authorizations);
            }
        }

        checkNotNull(element, "Could not find edge/vertex with id: " + elementId);
        return element;
    }

    private void publishVertex(Vertex vertex, String action, Authorizations authorizations, String workspaceId, User user) throws IOException {
        if (action.equals("delete")) {
            graph.removeVertex(vertex, authorizations);
            return;
        }

        LOGGER.debug("publishing vertex %s(%s)", vertex.getId(), vertex.getVisibility().toString());
        Visibility originalVertexVisibility = vertex.getVisibility();
        JSONObject visibilityJson = LumifyProperties.VISIBILITY_SOURCE.getPropertyValue(vertex);
        JSONArray workspaceJsonArray = JSONUtil.getOrCreateJSONArray(visibilityJson, VisibilityTranslator.JSON_WORKSPACES);
        if (!JSONUtil.arrayContains(workspaceJsonArray, workspaceId)) {
            throw new LumifyException(String.format("vertex with id '%s' is not local to workspace '%s'", vertex.getId(), workspaceId));
        }

        visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJson);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        ExistingElementMutation<Vertex> vertexElementMutation = vertex.prepareMutation();
        vertexElementMutation.alterElementVisibility(lumifyVisibility.getVisibility());

        for (Property property : vertex.getProperties()) {
            OntologyProperty ontologyProperty = ontologyRepository.getProperty(property.getName());
            checkNotNull(ontologyProperty, "Could not find property " + property.getName());
            if (!ontologyProperty.getUserVisible() && !property.getName().equals(LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName())) {
                publishProperty(vertexElementMutation, property, workspaceId, user);
            }
        }

        Map<String, Object> metadata = new HashMap<String, Object>();
        // we need to alter the visibility of the json property, otherwise we'll have two json properties, one with the old visibility and one with the new.
        LumifyProperties.VISIBILITY_SOURCE.alterVisibility(vertexElementMutation, lumifyVisibility.getVisibility());
        LumifyProperties.VISIBILITY_SOURCE.setMetadata(metadata, visibilityJson);
        LumifyProperties.VISIBILITY_SOURCE.setProperty(vertexElementMutation, visibilityJson, metadata, lumifyVisibility.getVisibility());
        vertexElementMutation.save(authorizations);

        auditRepository.auditVertex(AuditAction.PUBLISH, vertex.getId(), "", "", user, lumifyVisibility.getVisibility());

        ModelUserContext systemModelUser = userRepository.getModelUserContext(authorizations, LumifyVisibility.SUPER_USER_VISIBILITY_STRING);
        for (Audit row : auditRepository.findByRowStartsWith(vertex.getId(), systemModelUser)) {
            auditRepository.updateColumnVisibility(row, originalVertexVisibility, lumifyVisibility.getVisibility().getVisibilityString());
        }
    }

    private void publishProperty(Element element, String action, String key, String name, String workspaceId, User user, Authorizations authorizations) {
        if (action.equals("delete")) {
            element.removeProperty(key, name, authorizations);
            return;
        }
        ExistingElementMutation elementMutation = element.prepareMutation();
        Iterable<Property> properties = element.getProperties(name);
        for (Property property : properties) {
            if (!property.getKey().equals(key)) {
                continue;
            }
            if (publishProperty(elementMutation, property, workspaceId, user)) {
                elementMutation.save(authorizations);
                return;
            }
        }
        throw new LumifyException(String.format("no property with key '%s' and name '%s' found on workspace '%s'", key, name, workspaceId));
    }

    private boolean publishProperty(ExistingElementMutation elementMutation, Property property, String workspaceId, User user) {
        JSONObject visibilityJson = LumifyProperties.VISIBILITY_SOURCE.getMetadataValue(property.getMetadata());
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
                .alterPropertyMetadata(property, LumifyProperties.VISIBILITY_SOURCE.getPropertyName(), visibilityJson.toString());

        auditRepository.auditEntityProperty(AuditAction.PUBLISH, elementMutation.getElement().getId(), property.getKey(),
                property.getName(), property.getValue(), property.getValue(), "", "", property.getMetadata(), user, lumifyVisibility.getVisibility());
        return true;
    }

    private void publishEdge(Edge edge, Vertex sourceVertex, Vertex destVertex, String action, String workspaceId, User user, Authorizations authorizations) {
        if (action.equals("delete")) {
            graph.removeEdge(edge, authorizations);
            return;
        }

        LOGGER.debug("publishing edge %s(%s)", edge.getId(), edge.getVisibility().toString());
        JSONObject visibilityJson = LumifyProperties.VISIBILITY_SOURCE.getPropertyValue(edge);
        JSONArray workspaceJsonArray = JSONUtil.getOrCreateJSONArray(visibilityJson, VisibilityTranslator.JSON_WORKSPACES);
        if (!JSONUtil.arrayContains(workspaceJsonArray, workspaceId)) {
            throw new LumifyException(String.format("edge with id '%s' is not local to workspace '%s'", edge.getId(), workspaceId));
        }

        if (edge.getLabel().equals(entityHasImageIri)) {
            publishGlyphIconProperty(edge, workspaceId, user, authorizations);
        }

        edge.removeProperty(LumifyProperties.VISIBILITY_SOURCE.getPropertyName(), authorizations);
        visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJson);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ExistingElementMutation<Edge> edgeExistingElementMutation = edge.prepareMutation();
        Visibility originalEdgeVisibility = edge.getVisibility();
        edgeExistingElementMutation.alterElementVisibility(lumifyVisibility.getVisibility());

        for (Property property : edge.getProperties()) {
            publishProperty(edgeExistingElementMutation, property, workspaceId, user);
        }

        auditRepository.auditEdgeElementMutation(AuditAction.PUBLISH, edgeExistingElementMutation, edge, sourceVertex, destVertex, "", user, lumifyVisibility.getVisibility());

        Map<String, Object> metadata = new HashMap<String, Object>();
        LumifyProperties.VISIBILITY_SOURCE.setMetadata(metadata, visibilityJson);
        LumifyProperties.VISIBILITY_SOURCE.setProperty(edgeExistingElementMutation, visibilityJson, metadata, lumifyVisibility.getVisibility());
        edge = edgeExistingElementMutation.save(authorizations);

        auditRepository.auditRelationship(AuditAction.PUBLISH, sourceVertex, destVertex, edge, "", "", user, edge.getVisibility());

        ModelUserContext systemUser = userRepository.getModelUserContext(authorizations, LumifyVisibility.SUPER_USER_VISIBILITY_STRING);
        for (Audit row : auditRepository.findByRowStartsWith(edge.getId(), systemUser)) {
            auditRepository.updateColumnVisibility(row, originalEdgeVisibility, lumifyVisibility.getVisibility().getVisibilityString());
        }

        for (Vertex termMention : termMentionRepository.findResolvedTo(destVertex.getId(), authorizations)) {
            termMentionRepository.updateVisibility(termMention, originalEdgeVisibility, lumifyVisibility.getVisibility(), authorizations);
        }
    }

    private void publishGlyphIconProperty(Edge hasImageEdge, String workspaceId, User user, Authorizations authorizations) {
        Vertex entityVertex = hasImageEdge.getVertex(Direction.OUT, authorizations);
        checkNotNull(entityVertex, "Could not find has image source vertex " + hasImageEdge.getVertexId(Direction.OUT));
        ExistingElementMutation elementMutation = entityVertex.prepareMutation();
        Iterable<Property> glyphIconProperties = entityVertex.getProperties(LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName());
        for (Property glyphIconProperty : glyphIconProperties) {
            if (publishProperty(elementMutation, glyphIconProperty, workspaceId, user)) {
                elementMutation.save(authorizations);
                return;
            }
        }
        LOGGER.warn("new has image edge without a glyph icon property being set on vertex %s", entityVertex.getId());
    }
}
