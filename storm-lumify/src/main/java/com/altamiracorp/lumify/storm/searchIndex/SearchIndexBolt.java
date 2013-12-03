package com.altamiracorp.lumify.storm.searchIndex;

import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.search.SearchProvider;
import com.altamiracorp.lumify.storm.BaseTextProcessingBolt;
import com.google.inject.Inject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class SearchIndexBolt extends BaseTextProcessingBolt {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexBolt.class);
    private SearchProvider searchProvider;
    private ArtifactRepository artifactRepository;
    private GraphRepository graphRepository;

    @Override
    protected void safeExecute(Tuple input) throws Exception {
        final JSONObject json = getJsonFromTuple(input);
        final String graphVertexId = json.getString("graphVertexId");

        GraphVertex graphVertex = graphRepository.findVertex(graphVertexId, getUser());
        if (graphVertex != null) {
            addGraphVertexToSearch(graphVertex);
            addTextToSearch(graphVertexId, graphVertex);
        } else {
            LOGGER.warn("Could not find vertex with id: " + graphVertexId);
        }

        getCollector().ack(input);
    }

    private void addGraphVertexToSearch(GraphVertex graphVertex) {
        graphRepository.save(graphVertex, getUser());
    }

    private void addTextToSearch(String graphVertexId, GraphVertex graphVertex) throws Exception {
        InputStream textInputStream = artifactRepository.getHighlightedText(graphVertex, getUser());
        if (textInputStream != null) {
            try {
                searchProvider.add(graphVertex, textInputStream);
            } finally {
                textInputStream.close();
            }
        } else {
            LOGGER.warn("Could not find vertex text with id: " + graphVertexId);
        }
    }

    @Inject
    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }

    @Inject
    public void setArtifactRepository(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    @Inject
    public void setGraphRepository(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }
}
