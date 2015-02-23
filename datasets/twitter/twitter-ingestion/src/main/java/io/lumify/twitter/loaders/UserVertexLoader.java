package io.lumify.twitter.loaders;

import static com.google.common.base.Preconditions.checkNotNull;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.twitter.TwitterOntology;

import java.util.concurrent.TimeUnit;

import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

/**
 * Responsible for loading Twitter user vertex information to/from the underlying data store
 */
public class UserVertexLoader {
    private final Cache<String, Vertex> userVertexCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final UserRepository userRepository;
    private final Authorizations authorizations;

    /**
     *
     * @param secureGraph The underlying graph data store instance, not null
     * @param workQueueRepo The work queue used to store pending operations, not null
     * @param userRepo The system user repository used for retrieving users known to the system, not null
     */
    @Inject
    public UserVertexLoader(final Graph secureGraph, final WorkQueueRepository workQueueRepo, final UserRepository userRepo) {
        graph = checkNotNull(secureGraph);
        workQueueRepository = checkNotNull(workQueueRepo);
        userRepository = checkNotNull(userRepo);

        authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());
    }

    /**
     * Loads the vertex corresponding to the provided vertex details.  If the vertex cannot be found,
     * a new one will be created.
     * @param userDetails The details corresponding to the vertex of interest, not null
     * @return The vertex corresponding to the details provided
     */
    public Vertex loadVertex(final UserVertexDetails userDetails) {
        checkNotNull(userDetails);

        final String profileImageUrl = userDetails.getProfileImageUrl();

        String vertexId = "TWITTER_USER_" + userDetails.getId();
        Vertex userVertex = userVertexCache.getIfPresent(vertexId);
        if( userVertex != null ) {
            return userVertex;
        }

        userVertex = graph.getVertex(vertexId, authorizations);
        if( userVertex == null ) {
            userVertex = createTwitterUserVertex(userDetails, vertexId);

            workQueueRepository.pushGraphPropertyQueue(userVertex, LumifyProperties.TITLE.getProperty(userVertex));

            if( !Strings.isNullOrEmpty(profileImageUrl) ) {
                workQueueRepository.pushGraphPropertyQueue(userVertex, TwitterOntology.PROFILE_IMAGE_URL.getProperty(userVertex));
            }

            workQueueRepository.pushGraphPropertyQueue(userVertex, TwitterOntology.SCREEN_NAME.getProperty(userVertex));
        }

        userVertexCache.put(vertexId, userVertex);

        return userVertex;
    }


    private Vertex createTwitterUserVertex(final UserVertexDetails userDetails, final String vertexId) {
        final VertexBuilder vertexBuilder = graph.prepareVertex(vertexId, LoaderConstants.EMPTY_VISIBILITY);

        // Set core ontology properties
        LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, TwitterOntology.CONCEPT_TYPE_USER, LoaderConstants.EMPTY_VISIBILITY);
        LumifyProperties.SOURCE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, LoaderConstants.SOURCE_NAME, LoaderConstants.EMPTY_VISIBILITY);
        LumifyProperties.TITLE.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, userDetails.getName(), LoaderConstants.EMPTY_VISIBILITY);


        // Add tweet properties
        TwitterOntology.SCREEN_NAME.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, userDetails.getScreenName(), LoaderConstants.EMPTY_VISIBILITY);

        final String profileImageUrl = userDetails.getProfileImageUrl();
        if( !Strings.isNullOrEmpty(profileImageUrl) ) {
            TwitterOntology.PROFILE_IMAGE_URL.addPropertyValue(vertexBuilder, LoaderConstants.MULTI_VALUE_KEY, profileImageUrl, LoaderConstants.EMPTY_VISIBILITY);
        }

        final Vertex userVertex = vertexBuilder.save(authorizations);
        graph.flush();

        return userVertex;
    }

}
