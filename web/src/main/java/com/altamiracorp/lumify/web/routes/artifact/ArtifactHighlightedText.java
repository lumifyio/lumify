package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

public class ArtifactHighlightedText extends BaseRequestHandler {
    private final ArtifactRepository artifactRepository;
    private final GraphRepository graphRepository;

    @Inject
    public ArtifactHighlightedText(final ArtifactRepository artifactRepository, GraphRepository graphRepository) {
        this.artifactRepository = artifactRepository;
        this.graphRepository = graphRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String graphVertexId = getAttributeString(request, "graphVertexId");
        GraphVertex artifactVertex = this.graphRepository.findVertex(graphVertexId, user);
        if (artifactVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        OutputStream out = response.getOutputStream();
        artifactRepository.writeHighlightedTextTo(artifactVertex, out, user);
        out.close();
    }
}
