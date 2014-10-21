package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspaceEdges implements ClientApiObject {
    private List<Edge> edges = new ArrayList<Edge>();

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    public static class Edge {
        private String from;
        private String to;
        private String relationshipType;
        private String id;
        private SandboxStatus diffType;
        private VisibilityJson visibilityJson;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getRelationshipType() {
            return relationshipType;
        }

        public void setRelationshipType(String relationshipType) {
            this.relationshipType = relationshipType;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public SandboxStatus getDiffType() {
            return diffType;
        }

        public void setDiffType(SandboxStatus diffType) {
            this.diffType = diffType;
        }

        public VisibilityJson getVisibilityJson() {
            return visibilityJson;
        }

        public void setVisibilityJson(VisibilityJson visibilityJson) {
            this.visibilityJson = visibilityJson;
        }
    }
}
