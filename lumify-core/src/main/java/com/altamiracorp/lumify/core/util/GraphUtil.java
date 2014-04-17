package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.model.PropertyJustificationMetadata;
import com.altamiracorp.lumify.core.model.PropertySourceMetadata;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.workspace.diff.SandboxStatus;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphUtil {
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

    public static void updateElementVisibilitySource(VisibilityTranslator visibilityTranslator, Element element, SandboxStatus sandboxStatus, String visibilitySource, String workspaceId) {
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
            JSONObject sourceObject,
            User user) {
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
        ExistingElementMutation<T> elementMutation = element.prepareMutation();

        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getMetadataValue(propertyMetadata);
        visibilityJson = updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId);
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setMetadata(propertyMetadata, visibilityJson);
        LumifyProperties.MODIFIED_DATE.setMetadata(propertyMetadata, new Date());
        LumifyProperties.MODIFIED_BY.setMetadata(propertyMetadata, user.getUserId());

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

        elementMutation.setProperty(propertyName, value, propertyMetadata, lumifyVisibility.getVisibility());
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

        if (workspaceId != null) {
            JSONArray workspacesJsonArray = JSONUtil.getOrCreateJSONArray(json, VisibilityTranslator.JSON_WORKSPACES);
            JSONUtil.addToJSONArrayIfDoesNotExist(workspacesJsonArray, workspaceId);
        }

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
