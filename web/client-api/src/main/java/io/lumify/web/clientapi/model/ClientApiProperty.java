package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.HashMap;
import java.util.Map;

public class ClientApiProperty implements ClientApiObject {
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

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}