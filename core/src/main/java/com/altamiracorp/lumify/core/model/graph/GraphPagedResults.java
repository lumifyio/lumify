package com.altamiracorp.lumify.core.model.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphPagedResults {
    private Map<String, List<GraphVertex>> results;
    private Map<String, Integer> count;

    public GraphPagedResults() {
        results = new HashMap<String, List<GraphVertex>>();
        count = new HashMap<String, Integer>();
    }

    public Map<String, List<GraphVertex>> getResults() {
        return results;
    }

    public void setResults(Map<String, List<GraphVertex>> results) {
        this.results = results;
    }

    public Map<String, Integer> getCount() {
        return count;
    }

    public void setCount(Map<String, Integer> count) {
        this.count = count;
    }
}
