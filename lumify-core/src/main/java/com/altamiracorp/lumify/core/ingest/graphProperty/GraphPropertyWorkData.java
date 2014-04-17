package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.securegraph.*;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GraphPropertyWorkData {
    private final Vertex vertex;
    private final Property property;
    private File localFile;

    public GraphPropertyWorkData(Vertex vertex, Property property) {
        this.vertex = vertex;
        this.property = property;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public Property getProperty() {
        return property;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public File getLocalFile() {
        return localFile;
    }

    public Visibility getVisibility() {
        return getVertex().getVisibility();
    }

    public Map<String, Object> getPropertyMetadata() {
        Map<String, Object> metadata = new HashMap<String, Object>();
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(getVertex());
        if (visibilityJson != null) {
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setMetadata(metadata, visibilityJson);
        }
        return metadata;
    }

    public void setVisibilityJsonOnElement(ElementBuilder builder) {
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(getVertex());
        if (visibilityJson != null) {
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(builder, visibilityJson, getVisibility());
        }
    }

    public void setVisibilityJsonOnElement(Element element) {
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(getVertex());
        if (visibilityJson != null) {
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(element, visibilityJson, getVisibility());
        }
    }
}
