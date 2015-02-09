package io.lumify.core.util;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.PropertyJustificationMetadata;
import io.lumify.core.model.SourceInfo;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.web.clientapi.model.SandboxStatus;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.mutation.ExistingElementMutation;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class GraphUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphUtil.class);
    public static final String LUMIFY_VERSION_KEY = "lumify.version";
    public static final int LUMIFY_VERSION = 3;
    public static final double SET_PROPERTY_CONFIDENCE = 0.5;
    public static final String SOURCE_INFO_VISIBILITY_STRING = "sourceInfo";

    public static void verifyVersion(Graph graph) {
        verifyVersion(graph, LUMIFY_VERSION);
    }

    public static void verifyVersion(Graph graph, int requiredVersion) {
        Object version = graph.getMetadata(LUMIFY_VERSION_KEY);
        if (version == null) {
            writeVersion(graph);
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
        public final ExistingElementMutation<T> elementMutation;
        public final LumifyVisibility visibility;

        public VisibilityAndElementMutation(LumifyVisibility visibility, ExistingElementMutation<T> elementMutation) {
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
            Authorizations authorizations
    ) {
        VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(element);
        visibilityJson = sandboxStatus != SandboxStatus.PUBLIC ? updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId) : updateVisibilitySource(visibilityJson, visibilitySource);

        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        ExistingElementMutation<T> m = element.<T>prepareMutation().alterElementVisibility(lumifyVisibility.getVisibility());
        if (LumifyProperties.VISIBILITY_JSON.getPropertyValue(element) != null) {
            Property visibilityJsonProperty = LumifyProperties.VISIBILITY_JSON.getProperty(element);
            m.alterPropertyVisibility(visibilityJsonProperty.getKey(), LumifyProperties.VISIBILITY_JSON.getPropertyName(), visibilityTranslator.getDefaultVisibility());
        }
        Metadata metadata = new Metadata();
        metadata.add(LumifyProperties.VISIBILITY_JSON.getPropertyName(), visibilityJson.toString(), visibilityTranslator.getDefaultVisibility());

        LumifyProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, metadata, visibilityTranslator.getDefaultVisibility());
        m.save(authorizations);
        return new VisibilityAndElementMutation<>(lumifyVisibility, m);
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
            SourceInfo sourceInfo,
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
            removeSourceInfoEdge(graph, element, propertyKey, propertyName, lumifyVisibility, authorizations);
            LumifyProperties.JUSTIFICATION.setMetadata(propertyMetadata, propertyJustificationMetadata, lumifyVisibility.getVisibility());
        } else if (sourceInfo != null) {
            Vertex sourceVertex = graph.getVertex(sourceInfo.getVertexId(), authorizations);
            LumifyProperties.JUSTIFICATION.removeMetadata(propertyMetadata);
            addSourceInfoEdge(
                    graph,
                    element,
                    element.getId(),
                    propertyKey,
                    propertyName,
                    sourceInfo.getSnippet(),
                    sourceInfo.getTextPropertyKey(),
                    sourceInfo.getStartOffset(),
                    sourceInfo.getEndOffset(),
                    sourceVertex,
                    lumifyVisibility,
                    authorizations
            );
        }

        elementMutation.addPropertyValue(propertyKey, propertyName, value, propertyMetadata, lumifyVisibility.getVisibility());
        return new VisibilityAndElementMutation<>(lumifyVisibility, elementMutation);
    }

    public static void addJustification(
            Graph graph,
            Vertex vertex,
            String justificationText,
            SourceInfo sourceInfo,
            LumifyVisibility lumifyVisibility,
            Authorizations authorizations
    ) {
        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            removeSourceInfoEdgeFromVertex(graph, vertex.getId(), vertex.getId(), null, null, lumifyVisibility, authorizations);
            LumifyProperties.JUSTIFICATION.setProperty(vertex, propertyJustificationMetadata, lumifyVisibility.getVisibility(), authorizations);
        } else if (sourceInfo != null) {
            Vertex sourceVertex = graph.getVertex(sourceInfo.getVertexId(), authorizations);
            LumifyProperties.JUSTIFICATION.removeProperty(vertex, authorizations);
            addSourceInfoEdgeToVertex(
                    graph,
                    vertex,
                    sourceInfo.getVertexId(),
                    null,
                    null,
                    sourceInfo.getSnippet(),
                    sourceInfo.getTextPropertyKey(),
                    sourceInfo.getStartOffset(),
                    sourceInfo.getEndOffset(),
                    sourceVertex,
                    lumifyVisibility,
                    authorizations
            );
        }
    }

    private static <T extends Element> void addSourceInfoEdge(
            Graph graph,
            T element,
            String forElementId,
            String propertyKey,
            String propertyName,
            String snippet,
            String textPropertyKey,
            int startOffset,
            int endOffset,
            Vertex sourceVertex,
            LumifyVisibility lumifyVisibility,
            Authorizations authorizations
    ) {
        if (element instanceof Vertex) {
            addSourceInfoEdgeToVertex(
                    graph,
                    (Vertex) element,
                    forElementId,
                    propertyKey,
                    propertyName,
                    snippet,
                    textPropertyKey,
                    startOffset,
                    endOffset,
                    sourceVertex,
                    lumifyVisibility,
                    authorizations
            );
        } else {
            addSourceInfoEdgeToEdge(
                    graph,
                    (Edge) element,
                    forElementId,
                    propertyKey,
                    propertyName,
                    snippet,
                    textPropertyKey,
                    startOffset,
                    endOffset,
                    sourceVertex,
                    lumifyVisibility,
                    authorizations
            );
        }
    }

    private static void addSourceInfoEdgeToVertex(
            Graph graph,
            Vertex vertex,
            String forElementId,
            String propertyKey,
            String propertyName,
            String snippet,
            String textPropertyKey,
            int startOffset,
            int endOffset,
            Vertex sourceVertex,
            LumifyVisibility lumifyVisibility,
            Authorizations authorizations
    ) {
        Visibility visibility = lumifyVisibility.getVisibility();
        Visibility edgeVisibility = LumifyVisibility.and(visibility, SOURCE_INFO_VISIBILITY_STRING);
        String edgeId = vertex.getId() + "hasSource" + sourceVertex.getId();
        EdgeBuilder m = graph.prepareEdge(edgeId, vertex, sourceVertex, LumifyProperties.EDGE_LABEL_HAS_SOURCE, edgeVisibility);
        LumifyProperties.SOURCE_INFO_FOR_ELEMENT_ID.setProperty(m, forElementId, visibility);
        if (propertyKey == null) {
            propertyKey = "";
        }
        LumifyProperties.SOURCE_INFO_PROPERTY_KEY.setProperty(m, propertyKey, edgeVisibility);
        if (propertyName == null) {
            propertyName = "";
        }
        LumifyProperties.SOURCE_INFO_PROPERTY_NAME.setProperty(m, propertyName, edgeVisibility);
        LumifyProperties.SOURCE_INFO_PROPERTY_VISIBILITY.setProperty(m, visibility.getVisibilityString(), edgeVisibility);
        LumifyProperties.SOURCE_INFO_SNIPPET.setProperty(m, snippet, edgeVisibility);
        LumifyProperties.SOURCE_INFO_TEXT_PROPERTY_KEY.setProperty(m, textPropertyKey, edgeVisibility);
        LumifyProperties.SOURCE_INFO_START_OFFSET.setProperty(m, startOffset, edgeVisibility);
        LumifyProperties.SOURCE_INFO_END_OFFSET.setProperty(m, endOffset, edgeVisibility);
        Edge edge = m.save(authorizations);
        graph.flush();
        LOGGER.debug("added source info edge: %s", edge.getId());
        LOGGER.debug("added source info edge (out): %s", edge.getVertexId(Direction.OUT));
        LOGGER.debug("added source info edge (in): %s", edge.getVertexId(Direction.IN));
        LOGGER.debug("added source info edge (visibility): %s", edge.getVisibility().toString());
    }

    private static void addSourceInfoEdgeToEdge(
            Graph graph,
            Edge edge,
            String forElementId,
            String propertyKey,
            String propertyName,
            String snippet,
            String textPropertyKey,
            int startOffset,
            int endOffset,
            Vertex sourceVertex,
            LumifyVisibility lumifyVisibility,
            Authorizations authorizations
    ) {
        Vertex inVertex = edge.getVertex(Direction.IN, authorizations);
        Vertex outVertex = edge.getVertex(Direction.OUT, authorizations);
        addSourceInfoEdgeToVertex(
                graph,
                inVertex,
                forElementId,
                propertyKey,
                propertyName,
                snippet,
                textPropertyKey,
                startOffset,
                endOffset,
                sourceVertex,
                lumifyVisibility,
                authorizations
        );
        addSourceInfoEdgeToVertex(
                graph,
                outVertex,
                forElementId,
                propertyKey,
                propertyName,
                snippet,
                textPropertyKey,
                startOffset,
                endOffset,
                sourceVertex,
                lumifyVisibility,
                authorizations
        );
    }

    private static void removeSourceInfoEdge(Graph graph, Element element, String propertyKey, String propertyName, LumifyVisibility lumifyVisibility, Authorizations authorizations) {
        if (element instanceof Vertex) {
            removeSourceInfoEdgeFromVertex(graph, element.getId(), element.getId(), propertyKey, propertyName, lumifyVisibility, authorizations);
        } else {
            removeSourceInfoEdgeFromEdge(graph, (Edge) element, propertyKey, propertyName, lumifyVisibility, authorizations);
        }
    }

    private static void removeSourceInfoEdgeFromVertex(Graph graph, String vertexId, String sourceInfoElementId, String propertyKey, String propertyName, LumifyVisibility lumifyVisibility, Authorizations authorizations) {
        Edge sourceInfoEdge = findSourceInfoEdge(graph, vertexId, sourceInfoElementId, propertyKey, propertyName, lumifyVisibility.getVisibility(), authorizations);
        if (sourceInfoEdge != null) {
            graph.removeEdge(sourceInfoEdge, authorizations);
        }
    }

    private static void removeSourceInfoEdgeFromEdge(Graph graph, Edge edge, String propertyKey, String propertyName, LumifyVisibility lumifyVisibility, Authorizations authorizations) {
        String inVertexId = edge.getVertexId(Direction.IN);
        String outVertexId = edge.getVertexId(Direction.OUT);
        removeSourceInfoEdgeFromVertex(graph, inVertexId, edge.getId(), propertyKey, propertyName, lumifyVisibility, authorizations);
        removeSourceInfoEdgeFromVertex(graph, outVertexId, edge.getId(), propertyKey, propertyName, lumifyVisibility, authorizations);
    }

    private static Edge findSourceInfoEdge(Graph graph, String vertexId, String forElementId, String propertyKey, String propertyName, Visibility visibility, Authorizations authorizations) {
        if (propertyKey == null) {
            propertyKey = "";
        }
        if (propertyName == null) {
            propertyName = "";
        }
        AuthorizationRepository authorizationRepository = InjectHelper.getInstance(AuthorizationRepository.class);
        Authorizations authorizationsWithSourceInfo = authorizationRepository.createAuthorizations(authorizations, SOURCE_INFO_VISIBILITY_STRING);
        Vertex vertex = graph.getVertex(vertexId, EnumSet.of(FetchHint.OUT_EDGE_REFS), authorizationsWithSourceInfo);
        Iterable<Edge> hasSourceEdges = vertex.getEdges(Direction.OUT, LumifyProperties.EDGE_LABEL_HAS_SOURCE, authorizationsWithSourceInfo);
        for (Edge hasSourceEdge : hasSourceEdges) {
            if (!forElementId.equals(LumifyProperties.SOURCE_INFO_FOR_ELEMENT_ID.getPropertyValue(hasSourceEdge))) {
                continue;
            }
            if (!propertyKey.equals(LumifyProperties.SOURCE_INFO_PROPERTY_KEY.getPropertyValue(hasSourceEdge))) {
                continue;
            }
            if (!propertyName.equals(LumifyProperties.SOURCE_INFO_PROPERTY_NAME.getPropertyValue(hasSourceEdge))) {
                continue;
            }
            if (!visibility.toString().equals(LumifyProperties.SOURCE_INFO_PROPERTY_VISIBILITY.getPropertyValue(hasSourceEdge))) {
                continue;
            }
            return hasSourceEdge;
        }
        return null;
    }

    public static SourceInfo getSourceInfoForEdge(Graph graph, Edge edge, Authorizations authorizations) {
        String inVertexId = edge.getVertexId(Direction.IN);
        Edge sourceInfoEdge = findSourceInfoEdge(graph, inVertexId, edge.getId(), null, null, edge.getVisibility(), authorizations);
        return getSourceInfoFromSourceInfoEdge(sourceInfoEdge);
    }

    public static SourceInfo getSourceInfoForVertex(Graph graph, Vertex vertex, Authorizations authorizations) {
        Edge sourceInfoEdge = findSourceInfoEdge(graph, vertex.getId(), vertex.getId(), null, null, vertex.getVisibility(), authorizations);
        return getSourceInfoFromSourceInfoEdge(sourceInfoEdge);
    }

    public static SourceInfo getSourceInfoForEdgeProperty(Graph graph, Edge edge, String propertyKey, String propertyName, Visibility visibility, Authorizations authorizations) {
        String inVertexId = edge.getVertexId(Direction.IN);
        Edge sourceInfoEdge = findSourceInfoEdge(graph, inVertexId, edge.getId(), propertyKey, propertyName, visibility, authorizations);
        return getSourceInfoFromSourceInfoEdge(sourceInfoEdge);
    }

    public static SourceInfo getSourceInfoForVertexProperty(Graph graph, String vertexId, Property property, Authorizations authorizations) {
        Edge sourceInfoEdge = findSourceInfoEdge(graph, vertexId, vertexId, property.getKey(), property.getName(), property.getVisibility(), authorizations);
        return getSourceInfoFromSourceInfoEdge(sourceInfoEdge);
    }

    private static SourceInfo getSourceInfoFromSourceInfoEdge(Edge sourceInfoEdge) {
        if (sourceInfoEdge == null) {
            return null;
        }
        String vertexId = sourceInfoEdge.getVertexId(Direction.IN);
        String textPropertyKey = LumifyProperties.SOURCE_INFO_TEXT_PROPERTY_KEY.getPropertyValue(sourceInfoEdge);
        int startOffset = LumifyProperties.SOURCE_INFO_START_OFFSET.getPropertyValue(sourceInfoEdge);
        int endOffset = LumifyProperties.SOURCE_INFO_END_OFFSET.getPropertyValue(sourceInfoEdge);
        String snippet = LumifyProperties.SOURCE_INFO_SNIPPET.getPropertyValue(sourceInfoEdge);

        return new SourceInfo(vertexId, textPropertyKey, startOffset, endOffset, snippet);
    }

    private static void mergeMetadata(Metadata metadata, Metadata additionalMetadata) {
        if (additionalMetadata == null) {
            return;
        }
        for (Metadata.Entry entry : additionalMetadata.entrySet()) {
            metadata.add(entry.getKey(), entry.getValue(), entry.getVisibility());
        }
    }

    public static Vertex addVertex(
            Graph graph,
            String conceptType,
            String visibilitySource,
            String workspaceId,
            String justificationText,
            SourceInfo sourceInfo,
            VisibilityTranslator visibilityTranslator,
            Authorizations authorizations
    ) {
        VisibilityJson visibilityJson = updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        VertexBuilder vertexBuilder = graph.prepareVertex(lumifyVisibility.getVisibility());
        LumifyProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, lumifyVisibility.getVisibility());
        Metadata propertyMetadata = new Metadata();
        LumifyProperties.VISIBILITY_JSON.setMetadata(propertyMetadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, conceptType, propertyMetadata, lumifyVisibility.getVisibility());

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            LumifyProperties.JUSTIFICATION.setProperty(vertexBuilder, propertyJustificationMetadata, lumifyVisibility.getVisibility());
        } else if (sourceInfo != null) {
            LumifyProperties.JUSTIFICATION.removeProperty(vertexBuilder, lumifyVisibility.getVisibility());
        }

        Vertex vertex = vertexBuilder.save(authorizations);

        if (justificationText != null) {
            removeSourceInfoEdgeFromVertex(graph, vertex.getId(), vertex.getId(), null, null, lumifyVisibility, authorizations);
        } else if (sourceInfo != null) {
            Vertex sourceDataVertex = graph.getVertex(sourceInfo.getVertexId(), authorizations);
            addSourceInfoEdgeToVertex(
                    graph,
                    vertex,
                    vertex.getId(),
                    null,
                    null,
                    sourceInfo.getSnippet(),
                    sourceInfo.getTextPropertyKey(),
                    sourceInfo.getStartOffset(),
                    sourceInfo.getEndOffset(),
                    sourceDataVertex,
                    lumifyVisibility,
                    authorizations
            );
        }

        return vertex;
    }

    public static Edge addEdge(
            Graph graph,
            Vertex sourceVertex,
            Vertex destVertex,
            String predicateLabel,
            String justificationText,
            SourceInfo sourceInfo,
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

        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            LumifyProperties.JUSTIFICATION.setProperty(edgeBuilder, propertyJustificationMetadata, lumifyVisibility.getVisibility());
        } else if (sourceInfo != null) {
            LumifyProperties.JUSTIFICATION.removeProperty(edgeBuilder, lumifyVisibility.getVisibility());
        }

        Edge edge = edgeBuilder.save(authorizations);

        if (justificationText != null) {
            removeSourceInfoEdgeFromEdge(graph, edge, null, null, lumifyVisibility, authorizations);
        } else if (sourceInfo != null) {
            Vertex sourceDataVertex = graph.getVertex(sourceInfo.getVertexId(), authorizations);
            addSourceInfoEdgeToEdge(
                    graph,
                    edge,
                    edge.getId(),
                    null,
                    null,
                    sourceInfo.getSnippet(),
                    sourceInfo.getTextPropertyKey(),
                    sourceInfo.getStartOffset(),
                    sourceInfo.getEndOffset(),
                    sourceDataVertex,
                    lumifyVisibility,
                    authorizations
            );
        }

        return edge;
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
