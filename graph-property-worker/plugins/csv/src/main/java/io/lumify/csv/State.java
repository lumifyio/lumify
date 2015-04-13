package io.lumify.csv;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.csv.model.Mapping;
import org.apache.commons.csv.CSVRecord;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;

import java.util.HashMap;
import java.util.Map;

public class State {
    private final Mapping mapping;
    private final GraphPropertyWorkData data;
    private CSVRecord record;
    private Map<String, String> vertexIdCache = new HashMap<String, String>();
    private Cache<String, Vertex> vertexCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build();

    public State(Mapping mapping, GraphPropertyWorkData data) {
        this.mapping = mapping;
        this.data = data;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public GraphPropertyWorkData getData() {
        return data;
    }

    public void setRecord(CSVRecord record) {
        this.record = record;
    }

    public CSVRecord getRecord() {
        return record;
    }

    public String getCachedVertexId(String hash) {
        return vertexIdCache.get(hash);
    }

    public void addCachedVertex(String hash, Vertex entityVertex) {
        this.vertexIdCache.put(hash, entityVertex.getId());
        this.vertexCache.put(hash, entityVertex);
    }

    public Vertex getVertex(Graph graph, String hash, Authorizations authorizations) {
        Vertex vertex = vertexCache.getIfPresent(hash);
        if (vertex != null) {
            return vertex;
        }

        String vertexId = getCachedVertexId(hash);
        if (vertexId != null) {
            return graph.getVertex(vertexId, authorizations);
        }

        return null;
    }
}
