package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.ingest.video.VideoTranscript;
import com.altamiracorp.lumify.core.model.PropertyJustificationMetadata;
import com.altamiracorp.lumify.core.model.PropertySourceMetadata;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.model.workspace.diff.SandboxStatus;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.altamiracorp.securegraph.type.GeoPoint;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;
import static com.google.common.base.Preconditions.checkNotNull;

public class JsonSerializer {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(JsonSerializer.class);

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
            return toJsonElement(vertex, workspaceId);
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
        json.put("sandboxStatus", GraphUtil.getSandboxStatus(element, workspaceId).toString());
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
        SandboxStatus[] sandboxStatuses = GraphUtil.getPropertySandboxStatuses(propertiesList, workspaceId);
        VideoTranscript allVideoTranscripts = new VideoTranscript();
        for (int i = 0; i < propertiesList.size(); i++) {
            Property property = propertiesList.get(i);
            allVideoTranscripts = mergePropertyIntoTranscript(propertiesList, property, allVideoTranscripts);
            JSONObject propertyJson = toJsonProperty(property);
            propertyJson.put("sandboxStatus", sandboxStatuses[i].toString());
            resultsJson.put(property.getName(), propertyJson);

            // TODO remove me and fix JavaScript to use full IRI name instead of just "title"
            if (LumifyProperties.TITLE.getKey().equals(property.getName())) {
                resultsJson.put("title", propertyJson);
            }
        }

        if (allVideoTranscripts.getEntries().size() > 0) {
            JSONObject videoTranscriptJson = new JSONObject();
            videoTranscriptJson.put("value", allVideoTranscripts.toJson());
            resultsJson.put(MediaLumifyProperties.VIDEO_TRANSCRIPT.getKey(), videoTranscriptJson);
        }

        return resultsJson;
    }

    // TODO remove this when the front end supports multiple video transcript properties
    private static VideoTranscript mergePropertyIntoTranscript(List<Property> propertiesList, Property property, VideoTranscript allVideoTranscripts) {
        if (property.getValue() instanceof StreamingPropertyValue) {
            try {
                if (property.getName().equals(MediaLumifyProperties.VIDEO_TRANSCRIPT.getKey())) {
                    allVideoTranscripts = mergeVideoTranscriptPropertyIntoTranscript(property, allVideoTranscripts);
                } else if (property.getName().equals(RawLumifyProperties.TEXT.getKey())) {
                    mergeTextPropertyIntoTranscript(propertiesList, property, allVideoTranscripts);
                }
            } catch (IOException ex) {
                LOGGER.error("Could not read video transcript from property %s", property.toString());
            }
        }
        return allVideoTranscripts;
    }

    private static void mergeTextPropertyIntoTranscript(List<Property> propertiesList, Property property, VideoTranscript allVideoTranscripts) throws IOException {
        String[] nameParts = RowKeyHelper.splitOnMajorFieldSeperator(property.getKey());
        if (nameParts.length != 3) {
            return;
        }

        String refPropertyName = nameParts[1];
        String refPropertyKey = nameParts[2];
        Property refProperty = findProperty(propertiesList, refPropertyName, refPropertyKey);
        if (refProperty == null) {
            return;
        }

        Long videoFrameStartTime = (Long) refProperty.getMetadata().get(MediaLumifyProperties.METADATA_VIDEO_FRAME_START_TIME);
        if (videoFrameStartTime == null) {
            return;
        }

        VideoTranscript.Time time = new VideoTranscript.Time(videoFrameStartTime, null);
        String text = IOUtils.toString(((StreamingPropertyValue) property.getValue()).getInputStream());
        allVideoTranscripts.add(time, text);
    }

    private static VideoTranscript mergeVideoTranscriptPropertyIntoTranscript(Property property, VideoTranscript allVideoTranscripts) throws IOException {
        String videoTranscriptJsonString = IOUtils.toString(((StreamingPropertyValue) property.getValue()).getInputStream());
        JSONObject videoTranscriptJson = new JSONObject(videoTranscriptJsonString);
        VideoTranscript videoTranscript = new VideoTranscript(videoTranscriptJson);
        allVideoTranscripts = allVideoTranscripts.merge(videoTranscript);
        return allVideoTranscripts;
    }

    private static Property findProperty(Iterable<Property> properties, String propertyName, String propertyKey) {
        for (Property property : properties) {
            if (property.getName().equals(propertyName) && property.getKey().equals(propertyKey)) {
                return property;
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
