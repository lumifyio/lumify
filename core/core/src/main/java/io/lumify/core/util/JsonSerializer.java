package io.lumify.core.util;

import io.lumify.core.ingest.video.VideoFrameInfo;
import io.lumify.core.ingest.video.VideoPropertyHelper;
import io.lumify.core.ingest.video.VideoTranscript;
import io.lumify.core.model.PropertyJustificationMetadata;
import io.lumify.core.model.PropertySourceMetadata;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.web.clientapi.model.SandboxStatus;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.property.StreamingPropertyValue;
import org.securegraph.type.GeoPoint;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;

public class JsonSerializer {
    public static JSONArray toJson(Iterable<? extends Element> elements, String workspaceId, Authorizations authorizations) {
        JSONArray result = new JSONArray();
        for (Element element : elements) {
            result.put(toJson(element, workspaceId, authorizations));
        }
        return result;
    }

    public static JSONObject toJson(Element element, String workspaceId, Authorizations authorizations) {
        checkNotNull(element, "element cannot be null");
        if (element instanceof Vertex) {
            return toJsonVertex((Vertex) element, workspaceId, authorizations);
        }
        if (element instanceof Edge) {
            return toJsonEdge((Edge) element, workspaceId);
        }
        throw new RuntimeException("Unexpected element type: " + element.getClass().getName());
    }

    public static JSONObject toJsonVertex(Vertex vertex, String workspaceId, Authorizations authorizations) {
        try {
            JSONObject json = toJsonElement(vertex, workspaceId);
            JSONArray vertexEdgeLabelsJson = getVertexEdgeLabelsJson(vertex, authorizations);
            if (vertexEdgeLabelsJson != null) {
                json.put("edgeLabels", vertexEdgeLabelsJson);
            }
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONArray getVertexEdgeLabelsJson(Vertex vertex, Authorizations authorizations) {
        if (authorizations == null) {
            return null;
        }
        Iterable<String> edgeLabels = vertex.getEdgeLabels(Direction.BOTH, authorizations);
        JSONArray result = new JSONArray();
        for (String edgeLabel : edgeLabels) {
            result.put(edgeLabel);
        }
        return result;
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
        json.put("sandboxStatus", GraphUtil.getSandboxStatus(element, workspaceId).toString());
        VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(element);
        if (visibilityJson != null) {
            json.put("visibilitySource", visibilityJson.getSource());
        }

        return json;
    }

    public static JSONArray toJsonProperties(Iterable<Property> properties, String workspaceId) {
        JSONArray resultsJson = new JSONArray();
        List<Property> propertiesList = toList(properties);
        Collections.sort(propertiesList, new ConfidencePropertyComparator());
        SandboxStatus[] sandboxStatuses = GraphUtil.getPropertySandboxStatuses(propertiesList, workspaceId);
        for (int i = 0; i < propertiesList.size(); i++) {
            Property property = propertiesList.get(i);
            String sandboxStatus = sandboxStatuses[i].toString();
            VideoFrameInfo videoFrameInfo;
            if ((videoFrameInfo = VideoPropertyHelper.getVideoFrameInfoFromProperty(property)) != null) {
                String textDescription = (String) property.getMetadata().getValue(LumifyProperties.META_DATA_TEXT_DESCRIPTION);
                addVideoFramePropertyToResults(resultsJson, videoFrameInfo.getPropertyKey(), textDescription, sandboxStatus);
            } else {
                JSONObject propertyJson = toJsonProperty(property);
                propertyJson.put("sandboxStatus", sandboxStatus);
                resultsJson.put(propertyJson);
            }
        }

        return resultsJson;
    }


    public static VideoTranscript getSynthesisedVideoTranscription(Vertex artifactVertex, String propertyKey) throws IOException {
        VideoTranscript videoTranscript = new VideoTranscript();
        for (Property property : artifactVertex.getProperties()) {
            VideoFrameInfo videoFrameInfo = VideoPropertyHelper.getVideoFrameInfoFromProperty(property);
            if (videoFrameInfo == null) {
                continue;
            }
            if (videoFrameInfo.getPropertyKey().equals(propertyKey)) {
                Object value = property.getValue();
                String text;
                if (value instanceof StreamingPropertyValue) {
                    text = IOUtils.toString(((StreamingPropertyValue) value).getInputStream());
                } else {
                    text = value.toString();
                }
                videoTranscript.add(new VideoTranscript.Time(videoFrameInfo.getFrameStartTime(), videoFrameInfo.getFrameEndTime()), text);
            }
        }
        if (videoTranscript.getEntries().size() > 0) {
            return videoTranscript;
        }
        return null;
    }

    private static void addVideoFramePropertyToResults(JSONArray resultsJson, String propertyKey, String textDescription, String sandboxStatus) {
        JSONObject json = findProperty(resultsJson, MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName(), propertyKey);
        if (json == null) {
            json = new JSONObject();
            json.put("key", propertyKey);
            json.put("name", MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName());
            json.put("sandboxStatus", sandboxStatus);
            json.put(LumifyProperties.META_DATA_TEXT_DESCRIPTION, textDescription);
            json.put("streamingPropertyValue", true);
            resultsJson.put(json);
        }
    }

    private static JSONObject findProperty(JSONArray resultsJson, String propertyName, String propertyKey) {
        for (int i = 0; i < resultsJson.length(); i++) {
            JSONObject json = resultsJson.getJSONObject(i);
            if (json.getString("name").equals(propertyName)
                    && json.getString("key").equals(propertyKey)) {
                return json;
            }
        }
        return null;
    }

    public static JSONObject toJsonProperty(Property property) {
        JSONObject result = new JSONObject();
        result.put("key", property.getKey());
        result.put("name", property.getName());

        Object propertyValue = property.getValue();
        if (propertyValue instanceof StreamingPropertyValue) {
            result.put("streamingPropertyValue", true);
        } else {
            result.put("value", toJsonValue(propertyValue));
        }

        for (Metadata.Entry metadataEntry : property.getMetadata().entrySet()) {
            result.put(metadataEntry.getKey(), toJsonValue(metadataEntry.getValue()));
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
            if (geoPoint.getDescription() != null) {
                result.put("description", geoPoint.getDescription());
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
}
