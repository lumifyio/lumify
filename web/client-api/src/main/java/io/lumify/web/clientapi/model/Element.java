package io.lumify.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Edge.class, name = "edge"),
        @JsonSubTypes.Type(value = Vertex.class, name = "vertex")
})
public abstract class Element {
    private String id;
    private List<Property> properties = new ArrayList<Property>();
    private SandboxStatus sandboxStatus;
    private String visibilitySource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SandboxStatus getSandboxStatus() {
        return sandboxStatus;
    }

    public void setSandboxStatus(SandboxStatus sandboxStatus) {
        this.sandboxStatus = sandboxStatus;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public String getVisibilitySource() {
        return visibilitySource;
    }

    public void setVisibilitySource(String visibilitySource) {
        this.visibilitySource = visibilitySource;
    }

    public abstract String getType();

    public static class Property {
        private SandboxStatus sandboxStatus;
        private String key;
        private String name;
        private boolean streamingPropertyValue;
        private Map<String, Object> metadata = new HashMap<String, Object>();
        private Object value;

        public SandboxStatus getSandboxStatus() {
            return sandboxStatus;
        }

        public void setSandboxStatus(SandboxStatus sandboxStatus) {
            this.sandboxStatus = sandboxStatus;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isStreamingPropertyValue() {
            return streamingPropertyValue;
        }

        public void setStreamingPropertyValue(boolean streamingPropertyValue) {
            this.streamingPropertyValue = streamingPropertyValue;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public Object getValue() {
            return ClientApiConverter.fromClientApiValue(value);
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
