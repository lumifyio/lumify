package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;

public class ArtifactHighlightedText extends BaseRequestHandler {
    private final ArtifactRepository artifactRepository;
    private final Graph graph;

    @Inject
    public ArtifactHighlightedText(final ArtifactRepository artifactRepository, Graph graph) {
        this.artifactRepository = artifactRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String graphVertexId = getAttributeString(request, "graphVertexId");
        Vertex artifactVertex = this.graph.getVertex(graphVertexId, user.getAuthorizations());
        if (artifactVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        OutputStream out = response.getOutputStream();
        InputStream in = artifactRepository.getHighlightedText(artifactVertex, user);
        if (in == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            IOUtils.copy(in, out);
        } finally {
            in.close();
        }
        out.close();
    }
}
