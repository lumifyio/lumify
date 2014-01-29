package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;

public class ArtifactHighlightedText extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public ArtifactHighlightedText(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String graphVertexId = getAttributeString(request, "graphVertexId");
        Vertex artifactVertex = graph.getVertex(graphVertexId, user.getAuthorizations());
        if (artifactVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        OutputStream out = response.getOutputStream();

        StreamingPropertyValue textValue = (StreamingPropertyValue) artifactVertex.getPropertyValue(PropertyName.HIGHLIGHTED_TEXT.toString(), 0);
        if (textValue == null) {
            textValue = (StreamingPropertyValue) artifactVertex.getPropertyValue(PropertyName.TEXT.toString(), 0);
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
            IOUtils.copy(in, out);
        } finally {
            in.close();
        }
        out.close();
    }
}
