package com.altamiracorp.lumify.core.model.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArtifactSearchPagedResults {
    private Map<String, Collection<ArtifactSearchResult>> results;
    private Map<String, Integer> count;

    public ArtifactSearchPagedResults() {
        results = new HashMap<String, Collection<ArtifactSearchResult>>();
        count = new HashMap<String, Integer>();
    }

    public Map<String, Collection<ArtifactSearchResult>> getResults() {
        return results;
    }

    public void setResults(Map<String, Collection<ArtifactSearchResult>> results) {
        this.results = results;
    }

    public Map<String, Integer> getCount() {
        return count;
    }

    public void setCount(Map<String, Integer> count) {
        this.count = count;
    }
}
