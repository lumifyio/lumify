package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.model.GraphVertexSearchResult;
import org.json.JSONArray;

public class GraphApiExt extends GraphApi {
    public GraphVertexSearchResult vertexSearch(String query) throws ApiException {
        JSONArray filters = new JSONArray();
        return vertexSearch(query, filters, null, null, null, null, null);
    }

    public GraphVertexSearchResult vertexSearch(String query, JSONArray filters, Integer offset, Integer size, String conceptType, Boolean leafNodes, String relatedToVertexId) throws ApiException {
        return vertexSearch(query, filters.toString(), offset, size, conceptType, leafNodes, relatedToVertexId);
    }
}
