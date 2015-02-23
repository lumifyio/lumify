package io.lumify.twitter.loaders;

import static com.google.common.base.Preconditions.checkNotNull;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.twitter.TwitterOntology;
import io.lumify.web.clientapi.model.VisibilityJson;

import java.io.ByteArrayInputStream;
import java.util.Date;

import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Metadata;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.property.StreamingPropertyValue;

import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

import com.google.common.base.Charsets;
import com.google.inject.Inject;

/**
 * Responsible for loading Twitter tweet status vertex information to/from the underlying data store
 */
public final class TweetVertexLoader {

    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final UserRepository userRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final Authorizations authorizations;

    /**
     *
     * @param secureGraph The underlying graph data store instance, not null
     * @param workQueueRepo The work queue used to store pending operations, not null
     * @param userRepo The system user repository used for retrieving users known to the system, not null
     * @param translator The visibility expression translator, not null
     */
    @Inject
    public TweetVertexLoader(final Graph secureGraph, final WorkQueueRepository workQueueRepo,
                                final UserRepository userRepo, final VisibilityTranslator translator) {
        graph = checkNotNull(secureGraph);
        workQueueRepository = checkNotNull(workQueueRepo);
        userRepository = checkNotNull(userRepo);
        visibilityTranslator = checkNotNull(translator);

        authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());
    }

    /**
     * Loads the vertex corresponding to the provided tweet status.  If the vertex cannot be found,
     * a new one will be created.
     * @param status The status corresponding to the vertex of interest, not null
     * @return The vertex corresponding to the status provided
     */
    public Vertex loadVertex(final Status status) {
        checkNotNull(status);

        final String vertexId = "TWEET_" + status.getId();
        final Vertex tweetVertex = createTweetVertex(status, vertexId);

        workQueueRepository.pushGraphPropertyQueue(tweetVertex, LumifyProperties.RAW.getProperty(tweetVertex));
        workQueueRepository.pushGraphPropertyQueue(tweetVertex, LumifyProperties.TEXT.getProperty(tweetVertex));

        return tweetVertex;
    }

    private Vertex createTweetVertex(final Status parsedStatus, final String vertexId) {
        final VertexBuilder vertexBuilder = graph.prepareVertex(vertexId, LoaderConstants.EMPTY_VISIBILITY);

        LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, TwitterOntology.CONCEPT_TYPE_TWEET, LoaderConstants.EMPTY_VISIBILITY);
        LumifyProperties.SOURCE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, LoaderConstants.SOURCE_NAME, LoaderConstants.EMPTY_VISIBILITY);

        final String rawJson = TwitterObjectFactory.getRawJSON(parsedStatus);
        final StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(rawJson.getBytes(Charsets.UTF_8)), byte[].class);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, rawValue, LoaderConstants.EMPTY_VISIBILITY);

        final String statusText = parsedStatus.getText();
        StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(statusText.getBytes()), String.class);

        final Metadata textMetadata = new Metadata();
        textMetadata.add(LumifyProperties.META_DATA_TEXT_DESCRIPTION.getPropertyName(), "Tweet Text", visibilityTranslator.getDefaultVisibility());
        LumifyProperties.TEXT.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, textValue, textMetadata, LoaderConstants.EMPTY_VISIBILITY);

        final String title = parsedStatus.getUser().getName() + ": " + statusText;
        LumifyProperties.TITLE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, title, LoaderConstants.EMPTY_VISIBILITY);

        final Date publishedDate = parsedStatus.getCreatedAt();
        if( publishedDate != null ) {
            LumifyProperties.PUBLISHED_DATE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, publishedDate, LoaderConstants.EMPTY_VISIBILITY);
        }

        final VisibilityJson visibilityJson = new VisibilityJson();
        LumifyProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, LoaderConstants.EMPTY_VISIBILITY);


        final Vertex tweetVertex = vertexBuilder.save(authorizations);
        graph.flush();

        return tweetVertex;
    }
}
