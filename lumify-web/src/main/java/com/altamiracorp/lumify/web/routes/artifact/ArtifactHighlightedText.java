package com.altamiracorp.lumify.web.routes.artifact;

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
import java.io.InputStream;
import java.io.OutputStream;

import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.HIGHLIGHTED_TEXT;
import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.TEXT;

public class ArtifactHighlightedText extends BaseRequestHandler {
    private final Graph graph;
    private final UserRepository userRepository;

    @Inject
    public ArtifactHighlightedText(
            final Graph graph,
            final UserRepository userRepository) {
        this.graph = graph;
        this.userRepository = userRepository;
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
        OutputStream out = response.getOutputStream();

        StreamingPropertyValue textValue = HIGHLIGHTED_TEXT.getPropertyValue(artifactVertex);
        if (textValue == null) {
            textValue = TEXT.getPropertyValue(artifactVertex);
            if (textValue == null) {
                response.sendError(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
        }
        InputStream in = textValue.getInputStream();
        if (in == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            IOUtils.copy(in, out);
        } finally {
            in.close();
        }
        out.close();
    }
}
