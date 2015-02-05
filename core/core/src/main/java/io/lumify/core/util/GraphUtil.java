package io.lumify.core.util;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.PropertyJustificationMetadata;
import io.lumify.core.model.PropertySourceMetadata;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.web.clientapi.model.SandboxStatus;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;
import org.securegraph.mutation.ExistingElementMutation;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class GraphUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphUtil.class);
    public static final String LUMIFY_VERSION_KEY = "lumify.version";
    public static final int LUMIFY_VERSION = 2;
    public static final double SET_PROPERTY_CONFIDENCE = 0.5;

    public static void verifyVersion(Graph graph) {
        verifyVersion(graph, LUMIFY_VERSION);
    }

    public static void verifyVersion(Graph graph, int requiredVersion) {
        Object version = graph.getMetadata(LUMIFY_VERSION_KEY);
        if (version == null) {
            return;
        }
        if (!(version instanceof Integer)) {
            throw new LumifyException("Invalid " + LUMIFY_VERSION_KEY + " found. Expected Integer, found " + version.getClass().getName());
        }
        Integer versionInt = (Integer) version;
        if (versionInt != requiredVersion) {
            throw new LumifyException("Invalid " + LUMIFY_VERSION_KEY + " found. Expected " + requiredVersion + ", found " + versionInt);
        }
        LOGGER.info("Lumify graph version verified: %d", versionInt);
    }

    public static void writeVersion(Graph graph) {
        writeVersion(graph, LUMIFY_VERSION);
    }

    public static void writeVersion(Graph graph, int version) {
        graph.setMetadata(LUMIFY_VERSION_KEY, version);
        LOGGER.info("Wrote %s: %d", LUMIFY_VERSION_KEY, version);
    }

    public static SandboxStatus getSandboxStatus(Element element, String workspaceId) {
        VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(element);
        return getSandboxStatusFromVisibilityJsonString(visibilityJson, workspaceId);
    }

    public static SandboxStatus getSandboxStatusFromVisibilityString(String visibility, String workspaceId) {
        if (visibility == null) {
            return SandboxStatus.PUBLIC;
        }
        if (!visibility.contains(workspaceId)) {
            return SandboxStatus.PUBLIC;
        }
        return SandboxStatus.PRIVATE;
    }

    public static SandboxStatus getSandboxStatusFromVisibilityJsonString(VisibilityJson visibilityJson, String workspaceId) {
        if (visibilityJson == null) {
            return SandboxStatus.PUBLIC;
        }
        Set<String> workspacesList = visibilityJson.getWorkspaces();
        if (workspacesList == null || workspacesList.size() == 0) {
            return SandboxStatus.PUBLIC;
        }
        if (!workspacesList.contains(workspaceId)) {
            return SandboxStatus.PUBLIC;
        }
        return SandboxStatus.PRIVATE;
    }

    public static SandboxStatus[] getPropertySandboxStatuses(List<Property> properties, String workspaceId) {
        SandboxStatus[] sandboxStatuses = new SandboxStatus[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getMetadataValue(property.getMetadata());
            sandboxStatuses[i] = getSandboxStatusFromVisibilityJsonString(visibilityJson, workspaceId);
        }

        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            if (sandboxStatuses[i] != SandboxStatus.PRIVATE) {
                continue;
            }
            VisibilityJson propertyVisibilityJson = LumifyProperties.VISIBILITY_JSON.getMetadataValue(property.getMetadata());
            for (int j = 0; j < properties.size(); j++) {
                Property p = properties.get(j);
                VisibilityJson pVisibilityJson = LumifyProperties.VISIBILITY_JSON.getMetadataValue(p.getMetadata());
                if (i == j) {
                    continue;
                }

                if (sandboxStatuses[j] == SandboxStatus.PUBLIC &&
                        property.getName().equals(p.getName()) &&
                        property.getKey().equals(p.getKey()) &&
                        (propertyVisibilityJson == null || pVisibilityJson == null || (propertyVisibilityJson.getSource().equals(pVisibilityJson.getSource())))) {
                    sandboxStatuses[i] = SandboxStatus.PUBLIC_CHANGED;
                }
            }
        }

        return sandboxStatuses;
    }

    public static Metadata metadataStringToMap(String metadataString, Visibility visibility) {
        Metadata metadata = new Metadata();
        if (metadataString != null && metadataString.length() > 0) {
            JSONObject metadataJson = new JSONObject(metadataString);
            for (Object keyObj : metadataJson.keySet()) {
                String key = "" + keyObj;
                metadata.add(key, metadataJson.get(key), visibility);
            }
        }
        return metadata;
    }

    public static class VisibilityAndElementMutation<T extends Element> {
        public final ElementMutation<T> elementMutation;
        public final LumifyVisibility visibility;

        public VisibilityAndElementMutation(LumifyVisibility visibility, ElementMutation<T> elementMutation) {
            this.visibility = visibility;
            this.elementMutation = elementMutation;
        }
    }

    public static <T extends Element> VisibilityAndElementMutation<T> updateElementVisibilitySource(
            VisibilityTranslator visibilityTranslator,
            Element element,
            SandboxStatus sandboxStatus,
            String visibilitySource,
            String workspaceId,
            Authorizations authorizations) {
        VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(element);
        visibilityJson = sandboxStatus != SandboxStatus.PUBLIC ? updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId) : updateVisibilitySource(visibilityJson, visibilitySource);

        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        ExistingElementMutation m = element.prepareMutation().alterElementVisibility(lumifyVisibility.getVisibility());
        if (LumifyProperties.VISIBILITY_JSON.getPropertyValue(element) != null) {
            Property visibilityJsonProperty = LumifyProperties.VISIBILITY_JSON.getProperty(element);
            m.alterPropertyVisibility(visibilityJsonProperty.getKey(), LumifyProperties.VISIBILITY_JSON.getPropertyName(), visibilityTranslator.getDefaultVisibility());
        }
        Metadata metadata = new Metadata();
        metadata.add(LumifyProperties.VISIBILITY_JSON.getPropertyName(), visibilityJson.toString(), visibilityTranslator.getDefaultVisibility());

        LumifyProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, metadata, visibilityTranslator.getDefaultVisibility());
        m.save(authorizations);
        return new VisibilityAndElementMutation<T>(lumifyVisibility, m);
    }

    public static <T extends Element> VisibilityAndElementMutation<T> setProperty(
            Graph graph,
            T element,
            String propertyName,
            String propertyKey,
            Object value,
            Metadata metadata,
            String visibilitySource,
            String workspaceId,
            VisibilityTranslator visibilityTranslator,
            String justificationText,
            JSONObject sourceObject,
            User user,
            Authorizations authorizations) {
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.setSource(visibilitySource);
        visibilityJson.addWorkspace(workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        Property oldProperty = element.getProperty(propertyKey, propertyName, lumifyVisibility.getVisibility());
        Metadata propertyMetadata;
        if (oldProperty != null) {
            propertyMetadata = oldProperty.getMetadata();
            if (oldProperty.getName().equals(propertyName) && oldProperty.getKey().equals(propertyKey)) {
                element.removeProperty(propertyKey, propertyName, authorizations);
                graph.flush();
            }
        } else {
            propertyMetadata = new Metadata();
        }

        mergeMetadata(propertyMetadata, metadata);

        ExistingElementMutation<T> elementMutation = element.prepareMutation();

        visibilityJson = updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId);
        Date now = new Date();
        if (LumifyProperties.CREATE_DATE.getMetadataValue(propertyMetadata, null) == null) {
            LumifyProperties.CREATE_DATE.setMetadata(propertyMetadata, now, visibilityTranslator.getDefaultVisibility());
        }
        if (LumifyProperties.CREATED_BY.getMetadataValue(propertyMetadata, null) == null) {
            LumifyProperties.CREATED_BY.setMetadata(propertyMetadata, user.getUserId(), visibilityTranslator.getDefaultVisibility());
        }
        LumifyProperties.VISIBILITY_JSON.setMetadata(propertyMetadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        LumifyProperties.MODIFIED_DATE.setMetadata(propertyMetadata, now, visibilityTranslator.getDefaultVisibility());
        LumifyProperties.MODIFIED_BY.setMetadata(propertyMetadata, user.getUserId(), visibilityTranslator.getDefaultVisibility());
        LumifyProperties.CONFIDENCE.setMetadata(propertyMetadata, SET_PROPERTY_CONFIDENCE, visibilityTranslator.getDefaultVisibility());

        lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            propertyMetadata.remove(PropertySourceMetadata.PROPERTY_SOURCE_METADATA);
            propertyMetadata.add(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION, propertyJustificationMetadata, lumifyVisibility.getVisibility());
        } else if (sourceObject.length() > 0) {
            PropertySourceMetadata sourceMetadata = createPropertySourceMetadata(sourceObject);
            propertyMetadata.remove(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION);
            propertyMetadata.add(PropertySourceMetadata.PROPERTY_SOURCE_METADATA, sourceMetadata, lumifyVisibility.getVisibility());
        }

        elementMutation.addPropertyValue(propertyKey, propertyName, value, propertyMetadata, lumifyVisibility.getVisibility());
        return new VisibilityAndElementMutation<T>(lumifyVisibility, elementMutation);
    }

    private static void mergeMetadata(Metadata metadata, Metadata additionalMetadata) {
        if (additionalMetadata == null) {
            return;
        }
        for (Metadata.Entry entry : additionalMetadata.entrySet()) {
            metadata.add(entry.getKey(), entry.getValue(), entry.getVisibility());
        }
    }

    private static PropertySourceMetadata createPropertySourceMetadata(JSONObject sourceObject) {
        int startOffset = sourceObject.getInt("startOffset");
        int endOffset = sourceObject.getInt("endOffset");
        String vertexId = sourceObject.getString("vertexId");
        String snippet = sourceObject.getString("snippet");
        String textPropertyKey = sourceObject.getString("textPropertyKey");
        return new PropertySourceMetadata(vertexId, textPropertyKey, startOffset, endOffset, snippet);
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
            User user,
            Authorizations authorizations) {
        Date now = new Date();
        VisibilityJson visibilityJson = updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ElementBuilder<Edge> edgeBuilder = graph.prepareEdge(sourceVertex, destVertex, predicateLabel, lumifyVisibility.getVisibility());
        LumifyProperties.VISIBILITY_JSON.setProperty(edgeBuilder, visibilityJson, lumifyVisibility.getVisibility());
        LumifyProperties.CONCEPT_TYPE.setProperty(edgeBuilder, OntologyRepository.TYPE_RELATIONSHIP, lumifyVisibility.getVisibility());
        LumifyProperties.CREATE_DATE.setProperty(edgeBuilder, now, lumifyVisibility.getVisibility());
        LumifyProperties.CREATED_BY.setProperty(edgeBuilder, user.getUserId(), lumifyVisibility.getVisibility());
        LumifyProperties.MODIFIED_DATE.setProperty(edgeBuilder, now, lumifyVisibility.getVisibility());
        LumifyProperties.MODIFIED_BY.setProperty(edgeBuilder, user.getUserId(), lumifyVisibility.getVisibility());

        addJustificationToMutation(edgeBuilder, justificationText, sourceInfo, lumifyVisibility);

        return edgeBuilder.save(authorizations);
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

    // TODO remove me?
    public static VisibilityJson updateVisibilitySource(VisibilityJson visibilityJson, String visibilitySource) {
        if (visibilityJson == null) {
            visibilityJson = new VisibilityJson();
        }

        visibilityJson.setSource(visibilitySource);
        return visibilityJson;
    }

    // TODO remove me?
    public static VisibilityJson updateVisibilitySourceAndAddWorkspaceId(VisibilityJson visibilityJson, String visibilitySource, String workspaceId) {
        if (visibilityJson == null) {
            visibilityJson = new VisibilityJson();
        }

        visibilityJson.setSource(visibilitySource);

        if (workspaceId != null) {
            visibilityJson.addWorkspace(workspaceId);
        }

        return visibilityJson;
    }

    // TODO remove me?
    public static VisibilityJson updateVisibilityJsonRemoveFromWorkspace(VisibilityJson json, String workspaceId) {
        if (json == null) {
            json = new VisibilityJson();
        }

        json.getWorkspaces().remove(workspaceId);

        return json;
    }

    // TODO remove me?
    public static VisibilityJson updateVisibilityJsonRemoveFromAllWorkspace(VisibilityJson json) {
        if (json == null) {
            json = new VisibilityJson();
        }

        json.getWorkspaces().clear();

        return json;
    }
}
