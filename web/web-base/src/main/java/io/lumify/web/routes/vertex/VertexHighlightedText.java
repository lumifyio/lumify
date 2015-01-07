package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.EntityHighlighter;
import io.lumify.core.config.Configuration;
import io.lumify.core.ingest.video.VideoTranscript;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.io.IOUtils;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.property.StreamingPropertyValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexHighlightedText extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexHighlightedText.class);
    private final Graph graph;
    private final EntityHighlighter entityHighlighter;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexHighlightedText(
            final Graph graph,
            final UserRepository userRepository,
            final EntityHighlighter entityHighlighter,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final TermMentionRepository termMentionRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.entityHighlighter = entityHighlighter;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        Authorizations authorizationsWithTermMention = termMentionRepository.getAuthorizations(authorizations);
        String workspaceId = getActiveWorkspaceId(request);

        String graphVertexId = getRequiredParameter(request, "graphVertexId");
        String propertyKey = getRequiredParameter(request, "propertyKey");

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            respondWithNotFound(response);
            return;
        }

        StreamingPropertyValue textPropertyValue = LumifyProperties.TEXT.getPropertyValue(artifactVertex, propertyKey);
        if (textPropertyValue != null) {
            LOGGER.debug("returning text for vertexId:%s property:%s", artifactVertex.getId(), propertyKey);
            String highlightedText;
            String text = IOUtils.toString(textPropertyValue.getInputStream(), "UTF-8");
            if (text == null) {
                highlightedText = "";
            } else {
                Iterable<Vertex> termMentions = termMentionRepository.findBySourceGraphVertexAndPropertyKey(artifactVertex.getId(), propertyKey, authorizationsWithTermMention);
                highlightedText = entityHighlighter.getHighlightedText(text, termMentions, workspaceId, authorizationsWithTermMention);
            }

            respondWithHtml(response, highlightedText);
            return;
        }

        VideoTranscript videoTranscript = MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyValue(artifactVertex, propertyKey);
        if (videoTranscript != null) {
            LOGGER.debug("returning video transcript for vertexId:%s property:%s", artifactVertex.getId(), propertyKey);
            Iterable<Vertex> termMentions = termMentionRepository.findBySourceGraphVertexAndPropertyKey(artifactVertex.getId(), propertyKey, authorizations);
            VideoTranscript highlightedVideoTranscript = entityHighlighter.getHighlightedVideoTranscript(videoTranscript, termMentions, workspaceId, authorizations);
            respondWithJson(response, highlightedVideoTranscript.toJson());
            return;
        }

        videoTranscript = JsonSerializer.getSynthesisedVideoTranscription(artifactVertex, propertyKey);
        if (videoTranscript != null) {
            LOGGER.debug("returning synthesised video transcript for vertexId:%s property:%s", artifactVertex.getId(), propertyKey);
            Iterable<Vertex> termMentions = termMentionRepository.findBySourceGraphVertexAndPropertyKey(artifactVertex.getId(), propertyKey, authorizations);
            VideoTranscript highlightedVideoTranscript = entityHighlighter.getHighlightedVideoTranscript(videoTranscript, termMentions, workspaceId, authorizationsWithTermMention);
            respondWithJson(response, highlightedVideoTranscript.toJson());
            return;
        }

        respondWithNotFound(response);
    }
}
