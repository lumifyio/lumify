package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.model.PropertyJustificationMetadata;
import com.altamiracorp.lumify.core.model.PropertySourceMetadata;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceEntity;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceUser;
import com.altamiracorp.lumify.core.model.workspace.diff.SandboxStatus;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.altamiracorp.securegraph.type.GeoPoint;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.GEO_LOCATION;
import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.GEO_LOCATION_DESCRIPTION;
import static com.altamiracorp.securegraph.util.IterableUtils.toList;
import static com.google.common.base.Preconditions.checkNotNull;

public class GraphUtil {
    public static JSONArray toJson(Iterable<? extends Element> elements, String workspaceId) {
        JSONArray result = new JSONArray();
        for (Element element : elements) {
            result.put(toJson(element, workspaceId));
        }
        return result;
    }

    public static JSONObject toJson(Element element, String workspaceId) {
        checkNotNull(element, "element cannot be null");
        if (element instanceof Vertex) {
            return toJsonVertex((Vertex) element, workspaceId);
        }
        if (element instanceof Edge) {
            return toJsonEdge((Edge) element, workspaceId);
        }
        throw new RuntimeException("Unexpected element type: " + element.getClass().getName());
    }

    public static JSONObject toJsonVertex(Vertex vertex, String workspaceId) {
        try {
            JSONObject json = toJsonElement(vertex, workspaceId);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONObject toJsonEdge(Edge edge, String workspaceId) {
        try {
            JSONObject json = toJsonElement(edge, workspaceId);
            json.put("label", edge.getLabel());
            json.put("sourceVertexId", edge.getVertexId(Direction.OUT));
            json.put("destVertexId", edge.getVertexId(Direction.IN));
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONObject toJsonElement(Element element, String workspaceId) {
        JSONObject json = new JSONObject();
        json.put("id", element.getId());
        json.put("properties", toJsonProperties(element.getProperties(), workspaceId));
        json.put("sandboxStatus", getSandboxStatus(element, workspaceId).toString());
        if (element.getVisibility() != null) {
            json.put(LumifyVisibilityProperties.VISIBILITY_PROPERTY.getKey(), element.getVisibility().toString());
        }
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(element);
        if (visibilityJson != null) {
            json.put(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getKey(), visibilityJson);
        }

        return json;
    }

    public static JSONObject toJsonProperties(Iterable<Property> properties, String workspaceId) {
        JSONObject resultsJson = new JSONObject();
        List<Property> propertiesList = toList(properties);
        SandboxStatus[] sandboxStatuses = getPropertySandboxStatuses(propertiesList, workspaceId);
        for (int i = 0; i < propertiesList.size(); i++) {
            Property property = propertiesList.get(i);
            if (property.getValue() instanceof StreamingPropertyValue) {
                continue;
            }
            JSONObject propertyJson = GraphUtil.toJsonProperty(property);
            propertyJson.put("sandboxStatus", sandboxStatuses[i].toString());
            resultsJson.put(property.getName(), propertyJson);

            // TODO remove me and fix JavaScript to use full name
            if (LumifyProperties.TITLE.getKey().equals(property.getName())) {
                resultsJson.put("title", propertyJson);
            }
        }
        return resultsJson;
    }

    public static JSONObject toJsonProperty(Property property) {
        JSONObject result = new JSONObject();

        result.put("value", toJsonValue(property.getValue()));

        if (property.getVisibility() != null) {
            result.put(LumifyVisibilityProperties.VISIBILITY_PROPERTY.getKey(), property.getVisibility().toString());
        }
        for (String key : property.getMetadata().keySet()) {
            Object value = property.getMetadata().get(key);
            result.put(key, toJsonValue(value));
        }

        return result;
    }

    private static Object toJsonValue(Object value) {
        if (value instanceof Text) {
            value = ((Text) value).getText();
        }

        if (value instanceof GeoPoint) {
            GeoPoint geoPoint = (GeoPoint) value;
            JSONObject result = new JSONObject();
            result.put("latitude", geoPoint.getLatitude());
            result.put("longitude", geoPoint.getLongitude());
            if (geoPoint.getAltitude() != null) {
                result.put("altitude", geoPoint.getAltitude());
            }
            return result;
        } else if (value instanceof Date) {
            return ((Date) value).getTime();
        } else if (value instanceof PropertyJustificationMetadata) {
            return ((PropertyJustificationMetadata) value).toJson();
        } else if (value instanceof PropertySourceMetadata) {
            return ((PropertySourceMetadata) value).toJson();
        } else if (value instanceof String) {
            try {
                String valueString = (String) value;
                valueString = valueString.trim();
                if (valueString.startsWith("{") && valueString.endsWith("}")) {
                    return new JSONObject(valueString);
                }
            } catch (Exception ex) {
                // ignore this exception it just mean the string wasn't really json
            }
        }
        return value;
    }

    // TODO refactor this to remove WorkspaceRepository from parameters and pass in workspaceUsers, workspaceEntities, and hasWritePermissions
    public static JSONObject toJson(WorkspaceRepository workspaceRepository, Workspace workspace, User user, boolean includeVertices) {
        try {
            List<WorkspaceUser> workspaceUsers = workspaceRepository.findUsersWithAccess(workspace, user);
            boolean hasWritePermissions = workspaceRepository.hasWritePermissions(workspace, user);

            JSONObject workspaceJson = new JSONObject();
            workspaceJson.put("workspaceId", workspace.getId());
            workspaceJson.put("title", workspace.getDisplayTitle());
            workspaceJson.put("createdBy", workspaceRepository.getCreatorUserId(workspace, user));
            workspaceJson.put("isSharedToUser", !workspaceRepository.getCreatorUserId(workspace, user).equals(user.getUserId()));
            workspaceJson.put("isEditable", hasWritePermissions);

            JSONArray usersJson = new JSONArray();
            for (WorkspaceUser workspaceUser : workspaceUsers) {
                String userId = workspaceUser.getUserId();
                JSONObject userJson = new JSONObject();
                userJson.put("userId", userId);
                userJson.put("access", workspaceUser.getWorkspaceAccess().toString().toLowerCase());
                usersJson.put(userJson);
            }
            workspaceJson.put("users", usersJson);

            if (includeVertices) {
                List<WorkspaceEntity> workspaceEntities = workspaceRepository.findEntities(workspace, user);
                JSONObject entitiesJson = new JSONObject();
                for (WorkspaceEntity workspaceEntity : workspaceEntities) {
                    if (!workspaceEntity.isVisible()) {
                        continue;
                    }
                    JSONObject workspaceEntityJson = new JSONObject();
                    JSONObject graphPositionJson = new JSONObject();
                    graphPositionJson.put("x", workspaceEntity.getGraphPositionX());
                    graphPositionJson.put("y", workspaceEntity.getGraphPositionY());
                    workspaceEntityJson.put("graphPosition", graphPositionJson);
                    entitiesJson.put(workspaceEntity.getEntityVertexId().toString(), workspaceEntityJson);
                }
                workspaceJson.put("entities", entitiesJson);
            }

            return workspaceJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static SandboxStatus getSandboxStatus(Element element, String workspaceId) {
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(element);
        return getPropertySandboxStatusFromVisibilityJsonString(visibilityJson, workspaceId);
    }

    private static SandboxStatus getPropertySandboxStatusFromVisibilityJsonString(JSONObject visibilityJson, String workspaceId) {
        if (visibilityJson == null) {
            return SandboxStatus.PUBLIC;
        }
        JSONArray workspacesJsonArray = visibilityJson.optJSONArray(VisibilityTranslator.JSON_WORKSPACES);
        if (workspacesJsonArray == null) {
            return SandboxStatus.PUBLIC;
        }
        if (!JSONUtil.arrayContains(workspacesJsonArray, workspaceId)) {
            return SandboxStatus.PUBLIC;
        }
        return SandboxStatus.PRIVATE;
    }

    public static SandboxStatus[] getPropertySandboxStatuses(List<Property> properties, String workspaceId) {
        SandboxStatus[] sandboxStatuses = new SandboxStatus[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getMetadataValue(property.getMetadata());
            sandboxStatuses[i] = getPropertySandboxStatusFromVisibilityJsonString(visibilityJson, workspaceId);
        }

        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            if (sandboxStatuses[i] != SandboxStatus.PRIVATE) {
                continue;
            }
            for (int j = 0; j < properties.size(); j++) {
                Property p = properties.get(j);
                if (i == j || !property.getName().equals(p.getName())) {
                    continue;
                }
                if (sandboxStatuses[j] == SandboxStatus.PUBLIC) {
                    sandboxStatuses[i] = SandboxStatus.PUBLIC_CHANGED;
                }
            }
        }

        return sandboxStatuses;
    }

    public static class VisibilityAndElementMutation<T extends Element> {
        public final ElementMutation<T> elementMutation;
        public final LumifyVisibility visibility;

        public VisibilityAndElementMutation(LumifyVisibility visibility, ElementMutation<T> elementMutation) {
            this.visibility = visibility;
            this.elementMutation = elementMutation;
        }
    }

    public static void updateElementVisibilitySource(Graph graph, VisibilityTranslator visibilityTranslator, Element element, SandboxStatus sandboxStatus, String visibilitySource, String workspaceId) {
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(element);
        visibilityJson = sandboxStatus != SandboxStatus.PUBLIC ? updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId) : updateVisibilitySource(visibilityJson, visibilitySource);

        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        ExistingElementMutation m = element.prepareMutation().alterElementVisibility(lumifyVisibility.getVisibility());
        if (LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(element) != null) {
            m.alterPropertyVisibility(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getKey(), lumifyVisibility.getVisibility());
        }
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(m, visibilityJson, lumifyVisibility.getVisibility());
        m.save();
    }

    public static <T extends Element> VisibilityAndElementMutation<T> setProperty(
            T element,
            String propertyName,
            Object value,
            String visibilitySource,
            String workspaceId,
            VisibilityTranslator visibilityTranslator,
            String justificationText,
            JSONObject sourceObject) {
        Property oldProperty = element.getProperty(propertyName);
        Map<String, Object> propertyMetadata;
        if (oldProperty != null) {
            propertyMetadata = oldProperty.getMetadata();
            if (oldProperty.getName().equals(propertyName) && oldProperty.getValue().equals(value)) {
                element.removeProperty(propertyName);
            }
        } else {
            propertyMetadata = new HashMap<String, Object>();
        }
        ElementMutation<T> elementMutation = element.prepareMutation();

        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getMetadataValue(propertyMetadata);
        visibilityJson = updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId);
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setMetadata(propertyMetadata, visibilityJson);

        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            if (propertyMetadata.containsKey(PropertySourceMetadata.PROPERTY_SOURCE_METADATA)) {
                propertyMetadata.remove(PropertySourceMetadata.PROPERTY_SOURCE_METADATA);
            }
            propertyMetadata.put(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION, propertyJustificationMetadata);
        } else if (sourceObject.length() > 0) {
            PropertySourceMetadata sourceMetadata = createPropertySourceMetadata(sourceObject);
            if (propertyMetadata.containsKey(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION)) {
                propertyMetadata.remove(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION);
            }
            propertyMetadata.put(PropertySourceMetadata.PROPERTY_SOURCE_METADATA, sourceMetadata);
        }

        if (GEO_LOCATION.getKey().equals(propertyName)) {
            GeoPoint geoPoint = (GeoPoint) value;
            GEO_LOCATION.setProperty(elementMutation, geoPoint, propertyMetadata, lumifyVisibility.getVisibility());
            GEO_LOCATION_DESCRIPTION.setProperty(elementMutation, "", propertyMetadata, lumifyVisibility.getVisibility());
        } else {
            elementMutation.setProperty(propertyName, value, propertyMetadata, lumifyVisibility.getVisibility());
        }
        return new VisibilityAndElementMutation<T>(lumifyVisibility, elementMutation);
    }

    private static PropertySourceMetadata createPropertySourceMetadata(JSONObject sourceObject) {
        int startOffset = sourceObject.getInt("startOffset");
        int endOffset = sourceObject.getInt("endOffset");
        String vertexId = sourceObject.getString("vertexId");
        String snippet = sourceObject.getString("snippet");
        return new PropertySourceMetadata(startOffset, endOffset, vertexId, snippet);
    }

    public static Edge addEdge(
            Graph graph,
            Vertex sourceVertex,
            Vertex destVertex,
            String predicateLabel,
            String justificationText,
            String sourceInfo,
            String visibilitySource,
            String workspaceId,
            VisibilityTranslator visibilityTranslator,
            Authorizations authorizations) {
        JSONObject visibilityJson = updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ElementBuilder<Edge> edgeBuilder = graph.prepareEdge(sourceVertex, destVertex, predicateLabel, lumifyVisibility.getVisibility(), authorizations);
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(edgeBuilder, visibilityJson, lumifyVisibility.getVisibility());

        addJustificationToMutation(edgeBuilder, justificationText, sourceInfo, lumifyVisibility);

        return edgeBuilder.save();
    }

    public static <T extends Element> void addJustificationToMutation(ElementMutation<T> mutation, String justificationText, String sourceInfo, LumifyVisibility lumifyVisibility) {
        final JSONObject sourceJson;
        if (sourceInfo != null) {
            sourceJson = new JSONObject(sourceInfo);
        } else {
            sourceJson = new JSONObject();
        }

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            mutation.setProperty(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION, propertyJustificationMetadata.toJson().toString(), lumifyVisibility.getVisibility());
        } else if (sourceJson.length() > 0) {
            PropertySourceMetadata sourceMetadata = createPropertySourceMetadata(sourceJson);
            mutation.setProperty(PropertySourceMetadata.PROPERTY_SOURCE_METADATA, sourceMetadata.toJson().toString(), lumifyVisibility.getVisibility());
        }
    }

    public static JSONObject updateVisibilitySource(JSONObject json, String visibilitySource) {
        if (json == null) {
            json = new JSONObject();
        }

        json.put(VisibilityTranslator.JSON_SOURCE, visibilitySource);
        return json;
    }

    public static JSONObject updateVisibilitySourceAndAddWorkspaceId(JSONObject json, String visibilitySource, String workspaceId) {
        if (json == null) {
            json = new JSONObject();
        }

        json.put(VisibilityTranslator.JSON_SOURCE, visibilitySource);

        JSONArray workspacesJsonArray = JSONUtil.getOrCreateJSONArray(json, VisibilityTranslator.JSON_WORKSPACES);
        JSONUtil.addToJSONArrayIfDoesNotExist(workspacesJsonArray, workspaceId);

        return json;
    }

    public static JSONObject updateVisibilityJsonRemoveFromWorkspace(JSONObject json, String workspaceId) {
        if (json == null) {
            json = new JSONObject();
        }

        JSONArray workspaceJsonArray = JSONUtil.getOrCreateJSONArray(json, VisibilityTranslator.JSON_WORKSPACES);
        JSONUtil.removeFromJSONArray(workspaceJsonArray, workspaceId);

        return json;
    }

    public static JSONObject updateVisibilityJsonRemoveFromAllWorkspace(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }
        JSONArray workspaceJsonArray = JSONUtil.getOrCreateJSONArray(json, VisibilityTranslator.JSON_WORKSPACES);
        JSONUtil.removeWorkspacesFromJSONArray(workspaceJsonArray);

        return json;
    }
}
