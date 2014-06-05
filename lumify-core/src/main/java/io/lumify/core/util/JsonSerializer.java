package io.lumify.core.util;

import io.lumify.core.ingest.video.VideoTranscript;
import io.lumify.core.model.PropertyJustificationMetadata;
import io.lumify.core.model.PropertySourceMetadata;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.model.workspace.diff.SandboxStatus;
import io.lumify.core.security.LumifyVisibilityProperties;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;

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

    public static JSONArray toJsonProperties(Iterable<Property> properties, String workspaceId) {
        JSONArray resultsJson = new JSONArray();
        List<Property> propertiesList = toList(properties);
        Collections.sort(propertiesList, new ConfidencePropertyComparator());
        SandboxStatus[] sandboxStatuses = GraphUtil.getPropertySandboxStatuses(propertiesList, workspaceId);
        for (int i = 0; i < propertiesList.size(); i++) {
            Property property = propertiesList.get(i);
            String sandboxStatus = sandboxStatuses[i].toString();
            VideoFrameInfo videoFrameInfo;
            if ((videoFrameInfo = getVideoFrameInfoFromProperty(property)) != null) {
                String textDescription = (String) property.getMetadata().get(RawLumifyProperties.META_DATA_TEXT_DESCRIPTION);
                addVideoFramePropertyToResults(resultsJson, videoFrameInfo.propertyKey, textDescription, sandboxStatus);
            } else {
                JSONObject propertyJson = toJsonProperty(property);
                propertyJson.put("sandboxStatus", sandboxStatus);
                resultsJson.put(propertyJson);
            }
        }

        return resultsJson;
    }

    private static class VideoFrameInfo {
        public String propertyKey;
        public long frameStartTime;
    }

    private static VideoFrameInfo getVideoFrameInfoFromProperty(Property property) {
        Object mimeType = property.getMetadata().get(RawLumifyProperties.META_DATA_MIME_TYPE);
        if (mimeType == null || !mimeType.equals("text/plain")) {
            return null;
        }
        Pattern pattern = Pattern.compile("^(.*)" + RowKeyHelper.MINOR_FIELD_SEPARATOR + MediaLumifyProperties.VIDEO_FRAME.getKey() + RowKeyHelper.MINOR_FIELD_SEPARATOR + "([0-9]+)$");
        Matcher m = pattern.matcher(property.getKey());
        if (m.find()) {
            VideoFrameInfo videoFrameInfo = new VideoFrameInfo();
            videoFrameInfo.propertyKey = m.group(1);
            videoFrameInfo.frameStartTime = Long.parseLong(m.group(2));
            return videoFrameInfo;
        }
        return null;
    }

    public static VideoTranscript getSynthesisedVideoTranscription(Vertex artifactVertex, String propertyKey) throws IOException {
        VideoTranscript videoTranscript = new VideoTranscript();
        for (Property property : artifactVertex.getProperties()) {
            VideoFrameInfo videoFrameInfo = getVideoFrameInfoFromProperty(property);
            if (videoFrameInfo == null) {
                continue;
            }
            if (videoFrameInfo.propertyKey.equals(propertyKey)) {
                Object value = property.getValue();
                String text;
                if (value instanceof StreamingPropertyValue) {
                    text = IOUtils.toString(((StreamingPropertyValue) value).getInputStream());
                } else {
                    text = value.toString();
                }
                videoTranscript.add(new VideoTranscript.Time(videoFrameInfo.frameStartTime, null), text);
            }
        }
        if (videoTranscript.getEntries().size() > 0) {
            return videoTranscript;
        }
        return null;
    }

    private static void addVideoFramePropertyToResults(JSONArray resultsJson, String propertyKey, String textDescription, String sandboxStatus) {
        JSONObject json = findProperty(resultsJson, MediaLumifyProperties.VIDEO_TRANSCRIPT.getKey(), propertyKey);
        if (json == null) {
            json = new JSONObject();
            json.put("key", propertyKey);
            json.put("name", MediaLumifyProperties.VIDEO_TRANSCRIPT.getKey());
            json.put("sandboxStatus", sandboxStatus);
            json.put(RawLumifyProperties.META_DATA_TEXT_DESCRIPTION, textDescription);
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
