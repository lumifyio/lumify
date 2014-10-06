package io.lumify.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ArtifactImportResponse {
    private List<String> vertexIds = new ArrayList<String>();

    public List<String> getVertexIds() {
        return vertexIds;
    }

    @Override
    public String toString() {
        return "ArtifactImportResponse{" +
                "vertexIds=" + vertexIds +
                '}';
    }
}
