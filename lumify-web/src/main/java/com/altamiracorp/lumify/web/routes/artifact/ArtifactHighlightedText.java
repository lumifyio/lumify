package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.EntityHighlighter;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.TEXT;

public class ArtifactHighlightedText extends BaseRequestHandler {
    private final Graph graph;
    private final UserRepository userRepository;
    private final TermMentionRepository termMentionRepository;
    private final EntityHighlighter entityHighlighter;

    @Inject
    public ArtifactHighlightedText(
            final Graph graph,
            final UserRepository userRepository,
            final TermMentionRepository termMentionRepository,
            final EntityHighlighter entityHighlighter) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.termMentionRepository = termMentionRepository;
        this.entityHighlighter = entityHighlighter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

        String graphVertexId = getAttributeString(request, "graphVertexId");
        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String highlightedText;
        String text = getText(artifactVertex);
        if (text == null) {
            highlightedText = "";
        } else {
            Iterable<TermMentionModel> termMentions = termMentionRepository.findByGraphVertexId(artifactVertex.getId().toString(), user);
            highlightedText = entityHighlighter.getHighlightedText(text, termMentions);
        }

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        IOUtils.write(highlightedText, response.getOutputStream(), "UTF-8");
    }

    private String getText(Vertex artifactVertex) throws IOException {
        StreamingPropertyValue textPropertyValue = TEXT.getPropertyValue(artifactVertex);
        if (textPropertyValue == null) {
            return "";
        }
        return IOUtils.toString(textPropertyValue.getInputStream(), "UTF-8");
    }
}
