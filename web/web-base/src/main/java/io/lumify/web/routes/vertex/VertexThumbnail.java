package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.miniweb.utils.UrlUtils;
import io.lumify.web.BaseRequestHandler;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.property.StreamingPropertyValue;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

public class VertexThumbnail extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexThumbnail.class);

    private final ArtifactThumbnailRepository artifactThumbnailRepository;
    private final Graph graph;

    @Inject
    public VertexThumbnail(
            final ArtifactThumbnailRepository artifactThumbnailRepository,
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.artifactThumbnailRepository = artifactThumbnailRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            respondWithNotFound(response);
            return;
        }

        String widthStr = getOptionalParameter(request, "width");
        int[] boundaryDims = new int[]{200, 200};
        if (widthStr != null) {
            boundaryDims[0] = boundaryDims[1] = Integer.parseInt(widthStr);
        }

        byte[] thumbnailData;
        io.lumify.core.model.artifactThumbnails.ArtifactThumbnail thumbnail =
                artifactThumbnailRepository.getThumbnail(artifactVertex.getId(), "raw", boundaryDims[0], boundaryDims[1], user);
        if (thumbnail != null) {
            String format = thumbnail.getFormat();
            response.setContentType("image/" + format);
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + "." + format);
            setMaxAge(response, EXPIRES_1_HOUR);

            thumbnailData = thumbnail.getThumbnailData();
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: %s (raw) %d x %d", artifactVertex.getId(), boundaryDims[0], boundaryDims[1]);
                ServletOutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
                return;
            }
        }

        LOGGER.info("Cache miss for: %s (raw) %d x %d", artifactVertex.getId(), boundaryDims[0], boundaryDims[1]);
        StreamingPropertyValue rawPropertyValue = LumifyProperties.RAW.getPropertyValue(artifactVertex);
        if (rawPropertyValue == null) {
            respondWithNotFound(response);
            return;
        }

        InputStream in = rawPropertyValue.getInputStream();
        try {
            thumbnail = artifactThumbnailRepository.createThumbnail(artifactVertex, "raw", in, boundaryDims, user);

            String format = thumbnail.getFormat();
            response.setContentType("image/" + format);
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + "." + format);
            setMaxAge(response, EXPIRES_1_HOUR);

            thumbnailData = thumbnail.getThumbnailData();
        } finally {
            in.close();
        }
        ServletOutputStream out = response.getOutputStream();
        out.write(thumbnailData);
        out.close();
    }
}
