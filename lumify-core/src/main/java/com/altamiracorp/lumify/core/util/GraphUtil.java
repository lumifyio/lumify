package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.model.PropertyJustificationMetadata;
import com.altamiracorp.lumify.core.model.PropertySourceMetadata;
import com.altamiracorp.lumify.core.model.workspace.diff.SandboxStatus;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.altamiracorp.securegraph.type.GeoPoint;
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

public class GraphUtil {

    public static JSONArray toJson(Iterable<? extends Element> elements, String workspaceId) {
        JSONArray result = new JSONArray();
        for (Element element : elements) {
            result.put(toJson(element, workspaceId));
        }
        return result;
    }

    public static JSONObject toJson(Element element, String workspaceId) {
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
            json.put(LumifyVisibilityProperties.VISIBILITY_PROPERTY.toString(), element.getVisibility().toString());
        }
        String visibilityJson = (String) element.getPropertyValue(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString());
        if (visibilityJson != null && visibilityJson.length() > 0) {
            json.put(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), new JSONObject(visibilityJson));
        }

        return json;
    }

    public static Double[] parseLatLong(Object val) {
        Double[] result = new Double[2];
        if (val instanceof String) {
            String valStr = (String) val;
            String[] latlong = valStr.substring(valStr.indexOf('[') + 1, valStr.indexOf(']')).split(",");
            result[0] = Double.parseDouble(latlong[0]);
            result[1] = Double.parseDouble(latlong[1]);
        } else if (val instanceof GeoPoint) {
            GeoPoint valGeoPoint = (GeoPoint) val;
            result[0] = valGeoPoint.getLatitude();
            result[1] = valGeoPoint.getLongitude();
        }
        return result;
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
        }
        return resultsJson;
    }

    public static JSONObject toJsonProperty(Property property) {
        JSONObject result = new JSONObject();

        result.put("value", toJsonValue(property.getValue()));

        if (property.getVisibility() != null) {
            result.put(LumifyVisibilityProperties.VISIBILITY_PROPERTY.toString(), property.getVisibility().toString());
        }
        for (String key : property.getMetadata().keySet()) {
            Object value = property.getMetadata().get(key);
            result.put(key, toJsonValue(value));
        }

        return result;
    }

    private static Object toJsonValue(Object value) {
        if (value instanceof GeoPoint) {
            GeoPoint geoPoint = (GeoPoint) value;
            JSONObject result = new JSONObject();
            result.put("latitude", geoPoint.getLatitude());
            result.put("longitude", geoPoint.getLongitude());
            if (geoPoint.getAltitude() != null) {
                result.put("altitude", geoPoint.getAltitude());
            }
            return result;
        } else if (value instanceof Text) {
            return ((Text) value).getText();
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

    public static SandboxStatus getSandboxStatus(Element element, String workspaceId) {
        String visibilityJsonString = (String) element.getPropertyValue(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString());
        return getPropertySandboxStatusFromVisibilityJsonString(visibilityJsonString, workspaceId);
    }

    private static SandboxStatus getPropertySandboxStatusFromVisibilityJsonString(String visibilityJsonString, String workspaceId) {
        if (visibilityJsonString == null) {
            return SandboxStatus.PUBLIC;
        }
        JSONObject visibilityJson = new JSONObject(visibilityJsonString);
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
            String visibilityJsonString = (String) property.getMetadata().get(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString());
            sandboxStatuses[i] = getPropertySandboxStatusFromVisibilityJsonString(visibilityJsonString, workspaceId);
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
        String visibilityJsonString = (String) element.getPropertyValue(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString());
        JSONObject visibilityJson = sandboxStatus != SandboxStatus.PUBLIC ? updateVisibilitySourceAndAddWorkspaceId(visibilityJsonString, visibilitySource, workspaceId) : updateVisibilitySource(visibilityJsonString, visibilitySource);

        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        ExistingElementMutation m = element.prepareMutation().alterElementVisibility(lumifyVisibility.getVisibility());
        if (element.getProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString()) != null) {
            m.alterPropertyVisibility(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), lumifyVisibility.getVisibility());
        }
        m.setProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), visibilityJson.toString(), lumifyVisibility.getVisibility());
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
        } else {
            propertyMetadata = new HashMap<String, Object>();
        }
        ElementMutation<T> elementMutation = element.prepareMutation();

        String visibilityJsonString = (String) propertyMetadata.get(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString());
        JSONObject visibilityJson = updateVisibilitySourceAndAddWorkspaceId(visibilityJsonString, visibilitySource, workspaceId);
        propertyMetadata.put(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), visibilityJson.toString());

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
        ElementBuilder<Edge> edgeBuilder = graph.prepareEdge(sourceVertex, destVertex, predicateLabel, lumifyVisibility.getVisibility(), authorizations)
                .setProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), visibilityJson.toString(), lumifyVisibility.getVisibility());

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

    public static JSONObject updateVisibilitySource(String jsonString, String visibilitySource) {
        JSONObject json;
        if (jsonString == null) {
            json = new JSONObject();
        } else {
            json = new JSONObject(jsonString);
        }

        json.put(VisibilityTranslator.JSON_SOURCE, visibilitySource);
        return json;
    }

    public static JSONObject updateVisibilitySourceAndAddWorkspaceId(String jsonString, String visibilitySource, String workspaceId) {
        JSONObject json;
        if (jsonString == null) {
            json = new JSONObject();
        } else {
            json = new JSONObject(jsonString);
        }

        json.put(VisibilityTranslator.JSON_SOURCE, visibilitySource);

        JSONArray workspacesJsonArray = JSONUtil.getOrCreateJSONArray(json, VisibilityTranslator.JSON_WORKSPACES);
        JSONUtil.addToJSONArrayIfDoesNotExist(workspacesJsonArray, workspaceId);

        return json;
    }

    public static JSONObject updateVisibilityJsonRemoveFromWorkspace(String jsonString, String workspaceId) {
        JSONObject json;
        if (jsonString == null) {
            json = new JSONObject();
        } else {
            json = new JSONObject(jsonString);
        }

        JSONArray workspaceJsonArray = JSONUtil.getOrCreateJSONArray(json, VisibilityTranslator.JSON_WORKSPACES);
        JSONUtil.removeFromJSONArray(workspaceJsonArray, workspaceId);

        return json;
    }

    public static JSONObject updateVisibilityJsonRemoveFromAllWorkspace(String jsonString) {
        JSONObject json;
        if (jsonString == null) {
            json = new JSONObject();
        } else {
            json = new JSONObject(jsonString);
        }

        JSONArray workspaceJsonArray = JSONUtil.getOrCreateJSONArray(json, VisibilityTranslator.JSON_WORKSPACES);
        JSONUtil.removeWorkspacesFromJSONArray(workspaceJsonArray);

        return json;
    }
}
