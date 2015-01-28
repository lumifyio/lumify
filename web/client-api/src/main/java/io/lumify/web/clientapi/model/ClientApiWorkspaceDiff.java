package io.lumify.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspaceDiff implements ClientApiObject {
    private List<Item> diffs = new ArrayList<Item>();

    public void addAll(List<Item> diffs) {
        this.diffs.addAll(diffs);
    }

    public List<Item> getDiffs() {
        return diffs;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = VertexItem.class, name = "VertexDiffItem"),
            @JsonSubTypes.Type(value = EdgeItem.class, name = "EdgeDiffItem"),
            @JsonSubTypes.Type(value = PropertyItem.class, name = "PropertyDiffItem")
    })
    public abstract static class Item {
        private final String type;
        private final SandboxStatus sandboxStatus;
        private boolean deleted;

        protected Item(String type, SandboxStatus sandboxStatus, boolean deleted) {
            this.type = type;
            this.sandboxStatus = sandboxStatus;
            this.deleted = deleted;
        }

        public String getType() {
            return type;
        }

        public SandboxStatus getSandboxStatus() {
            return sandboxStatus;
        }

        @Override
        public String toString() {
            return ClientApiConverter.clientApiToString(this);
        }

        public boolean isDeleted() {
            return deleted;
        }
    }

    public static class EdgeItem extends Item {
        private String edgeId;
        private String label;
        private String outVertexId;
        private String inVertexId;
        private JsonNode visibilityJson;

        public EdgeItem() {
            super("EdgeDiffItem", SandboxStatus.PRIVATE, false);
        }

        public EdgeItem(String edgeId, String label, String outVertexId, String inVertexId, JsonNode visibilityJson, SandboxStatus sandboxStatus, boolean deleted) {
            super("EdgeDiffItem", sandboxStatus, deleted);
            this.edgeId = edgeId;
            this.label = label;
            this.outVertexId = outVertexId;
            this.inVertexId = inVertexId;
            this.visibilityJson = visibilityJson;
        }

        public String getEdgeId() {
            return edgeId;
        }

        public String getLabel() {
            return label;
        }

        public String getOutVertexId() {
            return outVertexId;
        }

        public String getInVertexId() {
            return inVertexId;
        }

        public JsonNode getVisibilityJson() {
            return visibilityJson;
        }
    }

    public static class VertexItem extends Item {
        private String conceptType;
        private String vertexId;
        private JsonNode visibilityJson;
        private boolean visible;
        private String title;

        public VertexItem() {
            super("VertexDiffItem", SandboxStatus.PRIVATE, false);
        }

        public VertexItem(
                String vertexId,
                String title,
                String conceptType,
                JsonNode visibilityJson,
                SandboxStatus sandboxStatus,
                boolean deleted,
                boolean visible
        ) {
            super("VertexDiffItem", sandboxStatus, deleted);
            this.vertexId = vertexId;
            this.visibilityJson = visibilityJson;
            this.visible = visible;
            this.title = title;
            this.conceptType = conceptType;
        }

        public String getVertexId() {
            return vertexId;
        }

        public JsonNode getVisibilityJson() {
            return visibilityJson;
        }

        public boolean isVisible() {
            return visible;
        }

        public String getTitle() {
            return title;
        }

        public String getConceptType() {
            return conceptType;
        }
    }

    public static class PropertyItem extends Item {
        private String elementType;
        private String elementId;
        private String name;
        private String key;
        private String visibilityString;
        @JsonProperty("old")
        private JsonNode oldData;

        @JsonProperty("new")
        private JsonNode newData;

        public PropertyItem() {
            super("PropertyDiffItem", SandboxStatus.PRIVATE, false);
        }

        public PropertyItem(String elementType, String elementId, String name, String key, JsonNode oldData, JsonNode newData, SandboxStatus sandboxStatus, boolean deleted, String visibilityString) {
            super("PropertyDiffItem", sandboxStatus, deleted);
            this.elementType = elementType;
            this.elementId = elementId;
            this.name = name;
            this.key = key;
            this.oldData = oldData;
            this.newData = newData;
            this.visibilityString = visibilityString;
        }

        public String getElementType() {
            return elementType;
        }

        public String getElementId() {
            return elementId;
        }

        public String getName() {
            return name;
        }

        public String getKey() {
            return key;
        }

        public JsonNode getOldData() {
            return oldData;
        }

        public JsonNode getNewData() {
            return newData;
        }

        public String getVisibilityString() {
            return visibilityString;
        }
    }
}
