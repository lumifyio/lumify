package io.lumify.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientApiEdge.class, name = "edge"),
        @JsonSubTypes.Type(value = ClientApiVertex.class, name = "vertex")
})
public abstract class ClientApiElement implements ClientApiObject {
    private String id;
    private List<ClientApiProperty> properties = new ArrayList<ClientApiProperty>();
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

    public List<ClientApiProperty> getProperties() {
        return properties;
    }

    public String getVisibilitySource() {
        return visibilitySource;
    }

    public void setVisibilitySource(String visibilitySource) {
        this.visibilitySource = visibilitySource;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }
}
