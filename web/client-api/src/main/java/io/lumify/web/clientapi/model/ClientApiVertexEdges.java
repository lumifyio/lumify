package io.lumify.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiVertexEdges implements ClientApiObject {
    private long totalReferences;
    private List<Edge> relationships = new ArrayList<Edge>();

    public long getTotalReferences() {
        return totalReferences;
    }

    public void setTotalReferences(long totalReferences) {
        this.totalReferences = totalReferences;
    }

    public List<Edge> getRelationships() {
        return relationships;
    }

    public static class Edge {
        private ClientApiEdge relationship;
        private ClientApiVertex vertex;

        public ClientApiEdge getRelationship() {
            return relationship;
        }

        public void setRelationship(ClientApiEdge relationship) {
            this.relationship = relationship;
        }

        public ClientApiVertex getVertex() {
            return vertex;
        }

        public void setVertex(ClientApiVertex vertex) {
            this.vertex = vertex;
        }
    }
}
