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

    public static class Edge extends ClientApiEdge {
        private String id;
        private SandboxStatus diffType;
        private VisibilityJson visibilityJson;
        
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
