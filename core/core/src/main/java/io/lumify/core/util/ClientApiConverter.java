package io.lumify.core.util;

import io.lumify.core.ingest.video.VideoFrameInfo;
import io.lumify.core.ingest.video.VideoPropertyHelper;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.web.clientapi.model.*;
import org.securegraph.*;
import org.securegraph.property.StreamingPropertyValue;
import org.securegraph.util.IterableUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientApiConverter extends io.lumify.web.clientapi.model.util.ClientApiConverter {
    public static ClientApiTermMentionsResponse toTermMentionsResponse(Iterable<Vertex> termMentions, String workspaceId, Authorizations authorizations) {
        ClientApiTermMentionsResponse termMentionsResponse = new ClientApiTermMentionsResponse();
        for (ClientApiElement element : toClientApi(termMentions, workspaceId, authorizations)) {
            termMentionsResponse.getTermMentions().add(element);
        }
        return termMentionsResponse;
    }

    public static List<ClientApiElement> toClientApi(Iterable<? extends org.securegraph.Element> elements, String workspaceId, Authorizations authorizations) {
        List<ClientApiElement> clientApiElements = new ArrayList<ClientApiElement>();
        for (org.securegraph.Element element : elements) {
            clientApiElements.add(toClientApi(element, workspaceId, authorizations));
        }
        return clientApiElements;
    }

    public static List<ClientApiVertex> toClientApiVertices(Iterable<? extends Vertex> vertices, String workspaceId, Authorizations authorizations) {
        List<ClientApiVertex> clientApiElements = new ArrayList<ClientApiVertex>();
        for (Vertex v : vertices) {
            clientApiElements.add(toClientApiVertex(v, workspaceId, authorizations));
        }
        return clientApiElements;
    }

    public static ClientApiElement toClientApi(org.securegraph.Element element, String workspaceId, Authorizations authorizations) {
        checkNotNull(element, "element cannot be null");
        if (element instanceof Vertex) {
            return toClientApiVertex((Vertex) element, workspaceId, authorizations);
        }
        if (element instanceof Edge) {
            return toClientApiEdge((Edge) element, workspaceId, authorizations);
        }
        throw new RuntimeException("Unexpected element type: " + element.getClass().getName());
    }

    public static ClientApiVertex toClientApiVertex(Vertex vertex, String workspaceId, Authorizations authorizations) {
        ClientApiVertex v = new ClientApiVertex();

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

    public static ClientApiEdge toClientApiEdge(Edge edge, String workspaceId, Authorizations authorizations) {
        ClientApiEdge e = new ClientApiEdge();
        populateClientApiEdge(e, edge, workspaceId, authorizations);
        return e;
    }

    public static ClientApiEdge toClientApiEdgeWithVertexData(Edge edge, Vertex source, Vertex target, String workspaceId, Authorizations authorizations) {
        ClientApiEdgeWithVertexData e = new ClientApiEdgeWithVertexData();
        e.setSource(toClientApiVertex(source, workspaceId, authorizations));
        e.setTarget(toClientApiVertex(target, workspaceId, authorizations));
        populateClientApiEdge(e, edge, workspaceId, authorizations);
        return e;
    }

    public static void populateClientApiEdge(ClientApiEdge e, Edge edge, String workspaceId, Authorizations authorizations) {
        e.setLabel(edge.getLabel());
        e.setSourceVertexId(edge.getVertexId(Direction.OUT));
        e.setDestVertexId(edge.getVertexId(Direction.IN));

        populateClientApiElement(e, edge, workspaceId, authorizations);
    }

    private static void populateClientApiElement(ClientApiElement clientApiElement, org.securegraph.Element element, String workspaceId, Authorizations authorizations) {
        clientApiElement.setId(element.getId());
        clientApiElement.getProperties().addAll(toClientApiProperties(element.getProperties(), workspaceId));
        clientApiElement.setSandboxStatus(GraphUtil.getSandboxStatus(element, workspaceId));

        VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(element);
        if (visibilityJson != null) {
            clientApiElement.setVisibilitySource(visibilityJson.getSource());
        }
    }

    public static List<ClientApiProperty> toClientApiProperties(Iterable<Property> properties, String workspaceId) {
        List<ClientApiProperty> clientApiProperties = new ArrayList<ClientApiProperty>();
        List<Property> propertiesList = IterableUtils.toList(properties);
        Collections.sort(propertiesList, new ConfidencePropertyComparator());
        SandboxStatus[] sandboxStatuses = GraphUtil.getPropertySandboxStatuses(propertiesList, workspaceId);
        for (int i = 0; i < propertiesList.size(); i++) {
            Property property = propertiesList.get(i);
            SandboxStatus sandboxStatus = sandboxStatuses[i];
            VideoFrameInfo videoFrameInfo;
            if ((videoFrameInfo = VideoPropertyHelper.getVideoFrameInfoFromProperty(property)) != null) {
                String textDescription = (String) property.getMetadata().getValue(LumifyProperties.META_DATA_TEXT_DESCRIPTION);
                addVideoFramePropertyToResults(clientApiProperties, videoFrameInfo.getPropertyKey(), textDescription, sandboxStatus);
            } else {
                ClientApiProperty clientApiProperty = toClientApiProperty(property);
                clientApiProperty.setSandboxStatus(sandboxStatus);
                clientApiProperties.add(clientApiProperty);
            }
        }
        return clientApiProperties;
    }

    public static ClientApiProperty toClientApiProperty(Property property) {
        ClientApiProperty clientApiProperty = new ClientApiProperty();
        clientApiProperty.setKey(property.getKey());
        clientApiProperty.setName(property.getName());

        Object propertyValue = property.getValue();
        if (propertyValue instanceof StreamingPropertyValue) {
            clientApiProperty.setStreamingPropertyValue(true);
        } else {
            clientApiProperty.setValue(toClientApiValue(propertyValue));
        }

        for (Metadata.Entry entry : property.getMetadata().entrySet()) {
            clientApiProperty.getMetadata().put(entry.getKey(), toClientApiValue(entry.getValue()));
        }

        return clientApiProperty;
    }

    private static void addVideoFramePropertyToResults(List<ClientApiProperty> clientApiProperties, String propertyKey, String textDescription, SandboxStatus sandboxStatus) {
        ClientApiProperty clientApiProperty = findProperty(clientApiProperties, MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName(), propertyKey);
        if (clientApiProperty == null) {
            clientApiProperty = new ClientApiProperty();
            clientApiProperty.setKey(propertyKey);
            clientApiProperty.setName(MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName());
            clientApiProperty.setSandboxStatus(sandboxStatus);
            clientApiProperty.getMetadata().put(LumifyProperties.META_DATA_TEXT_DESCRIPTION, textDescription);
            clientApiProperty.setStreamingPropertyValue(true);
            clientApiProperties.add(clientApiProperty);
        }
    }

    private static ClientApiProperty findProperty(List<ClientApiProperty> clientApiProperties, String propertyName, String propertyKey) {
        for (ClientApiProperty property : clientApiProperties) {
            if (property.getName().equals(propertyName) && property.getKey().equals(propertyKey)) {
                return property;
            }
        }
        return null;
    }
}
