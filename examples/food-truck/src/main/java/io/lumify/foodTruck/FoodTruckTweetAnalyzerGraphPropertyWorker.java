package io.lumify.foodTruck;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.twitter.TwitterOntology;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.apache.commons.io.IOUtils;
import org.securegraph.*;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.securegraph.util.IterableUtils.toList;

public class FoodTruckTweetAnalyzerGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FoodTruckTweetAnalyzerGraphPropertyWorker.class);
    private Cache<String, List<Vertex>> keywordVerticesCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    private static final String KEYWORD_VERTICES_CACHE_KEY = "keywords";

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String text = IOUtils.toString(in);
        Vertex tweetVertex = (Vertex) data.getElement();

        LOGGER.debug("processing tweet: %s", text);
        findAndLinkKeywords(tweetVertex, text, data.getVisibility(), data.getVisibilitySourceJson());
    }

    private void findAndLinkKeywords(Vertex tweetVertex, String text, Visibility visibility, VisibilityJson visibilityJson) {
        List<Vertex> keywordVertices = getKeywordVertices();
        for (Vertex keywordVertex : keywordVertices) {
            Iterable<String> keywords = FoodTruckOntology.KEYWORD.getPropertyValues(keywordVertex);
            for (String keyword : keywords) {
                int startOffset = text.toLowerCase().indexOf(keyword.toLowerCase());
                if (startOffset < 0) {
                    continue;
                }
                int endOffset = startOffset + keyword.length();

                createTermMentionAndEdge(tweetVertex, keywordVertex, keyword, startOffset, endOffset, visibility, visibilityJson);
            }
        }
    }

    private Edge createTermMentionAndEdge(Vertex tweetVertex, Vertex keywordVertex, String keyword, long startOffset, long endOffset, Visibility visibility, VisibilityJson visibilityJson) {
        String conceptUri = FoodTruckOntology.CONCEPT_TYPE_LOCATION;

        String edgeId = tweetVertex.getId() + "_HAS_" + keywordVertex.getId();
        Edge edge = getGraph().addEdge(edgeId, tweetVertex, keywordVertex, FoodTruckOntology.EDGE_LABEL_HAS_KEYWORD, visibility, getAuthorizations());
        getGraph().flush();

        new TermMentionBuilder()
                .sourceVertex(tweetVertex)
                .resolvedTo(keywordVertex, edge)
                .start(startOffset)
                .end(endOffset)
                .conceptIri(conceptUri)
                .title(keyword)
                .visibilityJson(visibilityJson)
                .save(getGraph(), getVisibilityTranslator(), getAuthorizations());
        getGraph().flush();

        getWorkQueueRepository().pushElement(edge);

        return edge;
    }

    private List<Vertex> getKeywordVertices() {
        List<Vertex> keywordVertices = keywordVerticesCache.getIfPresent(KEYWORD_VERTICES_CACHE_KEY);
        if (keywordVertices == null) {
            keywordVertices = toList(
                    getGraph()
                            .query(getAuthorizations())
                            .has(LumifyProperties.CONCEPT_TYPE.getPropertyName(), FoodTruckOntology.CONCEPT_TYPE_LOCATION)
                            .vertices()
            );
            keywordVerticesCache.put(KEYWORD_VERTICES_CACHE_KEY, keywordVertices);
        }
        return keywordVertices;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(LumifyProperties.TEXT.getPropertyName())) {
            return false;
        }
        if (!LumifyProperties.CONCEPT_TYPE.getPropertyValue(element).equals(TwitterOntology.CONCEPT_TYPE_TWEET)) {
            return false;
        }
        return true;
    }
}
