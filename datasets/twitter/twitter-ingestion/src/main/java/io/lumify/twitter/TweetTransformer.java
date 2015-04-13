package io.lumify.twitter;

import static com.google.common.base.Preconditions.checkNotNull;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.twitter.loaders.LoaderConstants;
import io.lumify.twitter.loaders.TweetVertexLoader;
import io.lumify.twitter.loaders.UserVertexDetails;
import io.lumify.twitter.loaders.UserVertexLoader;
import io.lumify.web.clientapi.model.VisibilityJson;

import java.util.concurrent.TimeUnit;

import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;

import twitter4j.HashtagEntity;
import twitter4j.RetweetStatusFactory;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

/**
 * Responsible for transforming a tweet {@link Status} to prepare it for loading into the data store
 */
public final class TweetTransformer {
    private static final String PROCESS_TWITTER_INGEST = "twitter-ingest";

    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final UserRepository userRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final Authorizations authorizations;
    private final UserVertexLoader userLoader;
    private final TweetVertexLoader tweetLoader;

    private final Cache<String, Vertex> urlVertexCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();
    private final Cache<String, Vertex> hashtagVertexCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

    /**
     *
     * @param secureGraph The underlying graph data store instance, not null
     * @param workQueueRepo The work queue used to store pending operations, not null
     * @param userRepo The system user repository used for retrieving users known to the system, not null
     * @param translator The visibility expression translator, not null
     * @param userVertexLoader The loader used for storing user vertices, not null
     * @param tweetVertexLoader The loader used for storing tweet status vertices, not null
     */
    @Inject
    public TweetTransformer(final Graph secureGraph, final WorkQueueRepository workQueueRepo,
                               final UserRepository userRepo, final VisibilityTranslator translator,
                               final UserVertexLoader userVertexLoader, final TweetVertexLoader tweetVertexLoader) {
        graph = checkNotNull(secureGraph);
        workQueueRepository = checkNotNull(workQueueRepo);
        userRepository = checkNotNull(userRepo);
        visibilityTranslator = checkNotNull(translator);
        userLoader = checkNotNull(userVertexLoader);
        tweetLoader = checkNotNull(tweetVertexLoader);

        authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());
    }


    /**
     * Transforms the content of the provided tweet status to the format required by the underlying data store
     * @param tweetStatus The status to process, not null
     * @return The vertex representing the transformed tweet status content
     */
    public Vertex transformTweetStatus(final Status tweetStatus) {
        checkNotNull(tweetStatus);

        final UserVertexDetails userDetails = UserVertexDetails.fromTweetStatus(tweetStatus);
        Vertex userVertex = userLoader.loadVertex(userDetails);
        Vertex tweetVertex = tweetLoader.loadVertex(tweetStatus);
        createTweetedEdge(userVertex, tweetVertex);

        processEntities(tweetVertex, tweetStatus);
        processRetweetStatus(tweetVertex, tweetStatus);

        return tweetVertex;
    }


    private void createTweetedEdge(final Vertex userVertex, final Vertex tweetVertex) {
        final String tweetedEdgeId = userVertex.getId() + "_TWEETED_" + tweetVertex.getId();

        graph.addEdge(tweetedEdgeId, userVertex, tweetVertex, TwitterOntology.EDGE_LABEL_TWEETED, LoaderConstants.EMPTY_VISIBILITY, authorizations);
        graph.flush();
    }


    private void processRetweetStatus(final Vertex tweetVertex, final Status tweetStatus) {
        Status retweetedStatus = tweetStatus.getRetweetedStatus();
        if( retweetedStatus == null ) {
            return;
        }

        retweetedStatus = RetweetStatusFactory.createRetweetedStatus(tweetStatus);

        final Vertex retweetedTweet = transformTweetStatus(retweetedStatus);
        final String retweetEdgeId = tweetVertex.getId() + "_RETWEET_" + retweetedTweet.getId();

        graph.addEdge(retweetEdgeId, retweetedTweet, tweetVertex, TwitterOntology.EDGE_LABEL_RETWEET, LoaderConstants.EMPTY_VISIBILITY, authorizations);
        graph.flush();
    }

    private void processEntities(final Vertex tweetVertex, final Status tweetStatus) {
        processHashtags(tweetVertex, tweetStatus.getHashtagEntities());
        processUrls(tweetVertex, tweetStatus.getURLEntities());
        processUserMentions(tweetVertex, tweetStatus.getUserMentionEntities());
        processUrls(tweetVertex, tweetStatus.getMediaEntities());
    }


    private void processUrls(Vertex tweetVertex, final URLEntity[] urlEntities) {
        for (final URLEntity urlEntity : urlEntities) {
            final Vertex urlVertex = getUrlVertex(urlEntity);
            final Edge edge = createReferencesUrlEdge(tweetVertex, urlVertex);

            createTermMention(tweetVertex, urlVertex, edge, TwitterOntology.CONCEPT_TYPE_URL, urlEntity.getStart(), urlEntity.getEnd());
        }
    }


    private Vertex getUrlVertex(final URLEntity urlEntity) {
        String url = urlEntity.getExpandedURL();
        if( url == null ) {
            url = urlEntity.getURL();
        }

        final String vertexId = "TWITTER_URL_" + url;

        Vertex urlVertex = urlVertexCache.getIfPresent(vertexId);
        if( urlVertex != null ) {
            return urlVertex;
        }

        urlVertex = graph.getVertex(vertexId, authorizations);
        if( urlVertex == null ) {
            VertexBuilder vertexBuilder = graph.prepareVertex(vertexId, LoaderConstants.EMPTY_VISIBILITY);

            LumifyProperties.CONCEPT_TYPE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, TwitterOntology.CONCEPT_TYPE_URL, LoaderConstants.EMPTY_VISIBILITY);
            LumifyProperties.SOURCE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, LoaderConstants.SOURCE_NAME, LoaderConstants.EMPTY_VISIBILITY);
            LumifyProperties.TITLE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, url, LoaderConstants.EMPTY_VISIBILITY);

            urlVertex = vertexBuilder.save(authorizations);
            graph.flush();

            workQueueRepository.pushGraphPropertyQueue(urlVertex, LumifyProperties.TITLE.getProperty(urlVertex));
        }

        urlVertexCache.put(vertexId, urlVertex);

        return urlVertex;
    }

    private Edge createReferencesUrlEdge(Vertex tweetVertex, Vertex urlVertex) {
        final String mentionedEdgeId = tweetVertex.getId() + "_REFURL_" + urlVertex.getId();
        final Edge edge = graph.addEdge(mentionedEdgeId, tweetVertex, urlVertex, TwitterOntology.EDGE_LABEL_REFERENCED_URL, LoaderConstants.EMPTY_VISIBILITY, authorizations);
        graph.flush();

        return edge;
    }



    private void processUserMentions(Vertex tweetVertex, final UserMentionEntity[] userMentionEntities) {
        for (final UserMentionEntity userMentionEntity : userMentionEntities) {
            final UserVertexDetails userDetails = UserVertexDetails.fromUserMention(userMentionEntity);
            final Vertex userVertex = userLoader.loadVertex(userDetails);
            final Edge edge = createMentionedEdge(tweetVertex, userVertex);

            createTermMention(tweetVertex, userVertex, edge, TwitterOntology.CONCEPT_TYPE_USER, userMentionEntity.getStart(), userMentionEntity.getEnd());
        }
    }


    private Edge createMentionedEdge(Vertex tweetVertex, Vertex userVertex) {
        final String mentionedEdgeId = tweetVertex.getId() + "_MENTIONED_" + userVertex.getId();
        final Edge edge = graph.addEdge(mentionedEdgeId, tweetVertex, userVertex, TwitterOntology.EDGE_LABEL_MENTIONED, LoaderConstants.EMPTY_VISIBILITY, authorizations);
        graph.flush();

        return edge;
    }



    private void processHashtags(Vertex tweetVertex, final HashtagEntity[] hashtagEntities) {
        for (final HashtagEntity hashtagEntity : hashtagEntities) {
            final Vertex hashtagVertex = getHashtagVertex(hashtagEntity);
            final Edge edge = createTaggedEdge(tweetVertex, hashtagVertex);

            createTermMention(tweetVertex, hashtagVertex, edge, TwitterOntology.CONCEPT_TYPE_HASHTAG, hashtagEntity.getStart(), hashtagEntity.getEnd());
        }
    }


    private Vertex getHashtagVertex(final HashtagEntity hashtagEntity) {
        final String hashtagText = hashtagEntity.getText();
        final String vertexId = "TWITTER_HASHTAG_" + hashtagText.toLowerCase();

        Vertex hashtagVertex = hashtagVertexCache.getIfPresent(vertexId);
        if( hashtagVertex != null ) {
            return hashtagVertex;
        }

        hashtagVertex = graph.getVertex(vertexId, authorizations);
        if( hashtagVertex == null ) {
            final VertexBuilder vertexBuilder = graph.prepareVertex(vertexId, LoaderConstants.EMPTY_VISIBILITY);

            LumifyProperties.CONCEPT_TYPE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, TwitterOntology.CONCEPT_TYPE_HASHTAG, LoaderConstants.EMPTY_VISIBILITY);
            LumifyProperties.SOURCE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, LoaderConstants.SOURCE_NAME, LoaderConstants.EMPTY_VISIBILITY);
            LumifyProperties.TITLE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, hashtagText, LoaderConstants.EMPTY_VISIBILITY);

            hashtagVertex = vertexBuilder.save(authorizations);
            graph.flush();

            workQueueRepository.pushGraphPropertyQueue(hashtagVertex, LumifyProperties.TITLE.getProperty(hashtagVertex));
        }

        hashtagVertexCache.put(vertexId, hashtagVertex);

        return hashtagVertex;
    }


    private Edge createTaggedEdge(Vertex tweetVertex, Vertex hashtagVertex) {
        final String mentionedEdgeId = tweetVertex.getId() + "_TAGGED_" + hashtagVertex.getId();
        final Edge edge = graph.addEdge(mentionedEdgeId, tweetVertex, hashtagVertex, TwitterOntology.EDGE_LABEL_TAGGED, LoaderConstants.EMPTY_VISIBILITY, authorizations);
        graph.flush();

        return edge;
    }


    private void createTermMention(Vertex tweetVertex, Vertex vertex, Edge edge, String conceptUri, final long startOffset, final long endOffset) {
        final VisibilityJson visibilitySource = new VisibilityJson();
        final String title = LumifyProperties.TITLE.getPropertyValue(vertex);

        new TermMentionBuilder()
            .sourceVertex(tweetVertex)
            .propertyKey(LoaderConstants.MULTI_VALUE_KEY)
            .start(startOffset)
            .end(endOffset)
            .title(title)
            .process(PROCESS_TWITTER_INGEST)
            .conceptIri(conceptUri)
            .visibilityJson(visibilitySource)
            .resolvedTo(vertex, edge)
            .save(graph, visibilityTranslator, authorizations);
    }
}
