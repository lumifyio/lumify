package io.lumify.core.util;

import io.lumify.core.ingest.video.VideoFrameInfo;
import io.lumify.core.ingest.video.VideoPropertyHelper;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.web.clientapi.model.Element;
import io.lumify.web.clientapi.model.SandboxStatus;
import io.lumify.web.clientapi.model.TermMentionsResponse;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.property.StreamingPropertyValue;
import org.securegraph.util.IterableUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientApiConverter extends io.lumify.web.clientapi.model.util.ClientApiConverter {
    public static TermMentionsResponse toTermMentionsResponse(Iterable<Vertex> termMentions, String workspaceId, Authorizations authorizations) {
        TermMentionsResponse termMentionsResponse = new TermMentionsResponse();
        for (io.lumify.web.clientapi.model.Element element : toClientApi(termMentions, workspaceId, authorizations)) {
            termMentionsResponse.getTermMentions().add(element);
        }
        return termMentionsResponse;
    }

    public static List<? extends Element> toClientApi(Iterable<? extends org.securegraph.Element> elements, String workspaceId, Authorizations authorizations) {
        List<io.lumify.web.clientapi.model.Element> clientApiElements = new ArrayList<Element>();
        for (org.securegraph.Element element : elements) {
            clientApiElements.add(toClientApi(element, workspaceId, authorizations));
        }
        return clientApiElements;
    }

    public static io.lumify.web.clientapi.model.Element toClientApi(org.securegraph.Element element, String workspaceId, Authorizations authorizations) {
        checkNotNull(element, "element cannot be null");
        if (element instanceof Vertex) {
            return toClientApiVertex((Vertex) element, workspaceId, authorizations);
        }
        if (element instanceof Edge) {
            return toClientApiEdge((Edge) element, workspaceId, authorizations);
        }
        throw new RuntimeException("Unexpected element type: " + element.getClass().getName());
    }

    public static io.lumify.web.clientapi.model.Vertex toClientApiVertex(Vertex vertex, String workspaceId, Authorizations authorizations) {
        io.lumify.web.clientapi.model.Vertex v = new io.lumify.web.clientapi.model.Vertex();

        List<String> vertexEdgeLabels = getVertexEdgeLabels(vertex, authorizations);
        if (vertexEdgeLabels != null) {
            v.getEdgeLabels().addAll(vertexEdgeLabels);
        }

        populateClientApiElement(v, vertex, workspaceId, authorizations);
        return v;
    }

    private static List<String> getVertexEdgeLabels(Vertex vertex, Authorizations authorizations) {
        if (authorizations == null) {
            return null;
        }
        Iterable<String> edgeLabels = vertex.getEdgeLabels(Direction.BOTH, authorizations);
        return IterableUtils.toList(edgeLabels);
    }

    private static io.lumify.web.clientapi.model.Edge toClientApiEdge(Edge edge, String workspaceId, Authorizations authorizations) {
        io.lumify.web.clientapi.model.Edge e = new io.lumify.web.clientapi.model.Edge();
        e.setLabel(edge.getLabel());
        e.setSourceVertexId(edge.getVertexId(Direction.OUT));
        e.setDestVertexId(edge.getVertexId(Direction.IN));

        populateClientApiElement(e, edge, workspaceId, authorizations);
        return e;
    }

    private static void populateClientApiElement(io.lumify.web.clientapi.model.Element clientApiElement, org.securegraph.Element element, String workspaceId, Authorizations authorizations) {
        clientApiElement.setId(element.getId());
        clientApiElement.getProperties().addAll(toClientApiProperties(element.getProperties(), workspaceId));
        clientApiElement.setSandboxStatus(GraphUtil.getSandboxStatus(element, workspaceId));

        JSONObject visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(element);
        if (visibilityJson != null) {
            clientApiElement.setVisibilitySource(VisibilityTranslator.getVisibilitySource(visibilityJson));
        }
    }

    public static List<io.lumify.web.clientapi.model.Property> toClientApiProperties(Iterable<Property> properties, String workspaceId) {
        List<io.lumify.web.clientapi.model.Property> clientApiProperties = new ArrayList<io.lumify.web.clientapi.model.Property>();
        List<Property> propertiesList = IterableUtils.toList(properties);
        Collections.sort(propertiesList, new ConfidencePropertyComparator());
        SandboxStatus[] sandboxStatuses = GraphUtil.getPropertySandboxStatuses(propertiesList, workspaceId);
        for (int i = 0; i < propertiesList.size(); i++) {
            Property property = propertiesList.get(i);
            SandboxStatus sandboxStatus = sandboxStatuses[i];
            VideoFrameInfo videoFrameInfo;
            if ((videoFrameInfo = VideoPropertyHelper.getVideoFrameInfoFromProperty(property)) != null) {
                String textDescription = (String) property.getMetadata().get(LumifyProperties.META_DATA_TEXT_DESCRIPTION);
                addVideoFramePropertyToResults(clientApiProperties, videoFrameInfo.getPropertyKey(), textDescription, sandboxStatus);
            } else {
                io.lumify.web.clientapi.model.Property clientApiProperty = toClientApiProperty(property);
                clientApiProperty.setSandboxStatus(sandboxStatus);
                clientApiProperties.add(clientApiProperty);
            }
        }
        return clientApiProperties;
    }

    public static io.lumify.web.clientapi.model.Property toClientApiProperty(Property property) {
        io.lumify.web.clientapi.model.Property clientApiProperty = new io.lumify.web.clientapi.model.Property();
        clientApiProperty.setKey(property.getKey());
        clientApiProperty.setName(property.getName());

        Object propertyValue = property.getValue();
        if (propertyValue instanceof StreamingPropertyValue) {
            clientApiProperty.setStreamingPropertyValue(true);
        } else {
            clientApiProperty.setValue(toClientApiValue(propertyValue));
        }

        for (String key : property.getMetadata().keySet()) {
            Object value = property.getMetadata().get(key);
            clientApiProperty.getMetadata().put(key, toClientApiValue(value));
        }

        return clientApiProperty;
    }

    private static void addVideoFramePropertyToResults(List<io.lumify.web.clientapi.model.Property> clientApiProperties, String propertyKey, String textDescription, SandboxStatus sandboxStatus) {
        io.lumify.web.clientapi.model.Property clientApiProperty = findProperty(clientApiProperties, MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName(), propertyKey);
        if (clientApiProperty == null) {
            clientApiProperty = new io.lumify.web.clientapi.model.Property();
            clientApiProperty.setKey(propertyKey);
            clientApiProperty.setName(MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName());
            clientApiProperty.setSandboxStatus(sandboxStatus);
            clientApiProperty.getMetadata().put(LumifyProperties.META_DATA_TEXT_DESCRIPTION, textDescription);
            clientApiProperty.setStreamingPropertyValue(true);
            clientApiProperties.add(clientApiProperty);
        }
    }

    private static io.lumify.web.clientapi.model.Property findProperty(List<io.lumify.web.clientapi.model.Property> clientApiProperties, String propertyName, String propertyKey) {
        for (io.lumify.web.clientapi.model.Property property : clientApiProperties) {
            if (property.getName().equals(propertyName) && property.getKey().equals(propertyKey)) {
                return property;
            }
        }
        return null;
    }
}
