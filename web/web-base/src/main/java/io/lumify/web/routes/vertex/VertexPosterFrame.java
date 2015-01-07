package io.lumify.web.routes.vertex;

import io.lumify.miniweb.HandlerChain;
import io.lumify.miniweb.utils.UrlUtils;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.io.IOUtils;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.property.StreamingPropertyValue;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

import static io.lumify.core.model.properties.MediaLumifyProperties.RAW_POSTER_FRAME;

public class VertexPosterFrame extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexPosterFrame.class);
    private final Graph graph;
    private final ArtifactThumbnailRepository artifactThumbnailRepository;

    @Inject
    public VertexPosterFrame(
            final Graph graph,
            final ArtifactThumbnailRepository artifactThumbnailRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.artifactThumbnailRepository = artifactThumbnailRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));

        String widthStr = getOptionalParameter(request, "width");
        int[] boundaryDims = new int[]{200, 200};

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            respondWithNotFound(response);
            return;
        }

        if (widthStr != null) {
            boundaryDims[0] = boundaryDims[1] = Integer.parseInt(widthStr);

            response.setContentType("image/jpeg");
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + ".jpg");
            setMaxAge(response, EXPIRES_1_HOUR);

            byte[] thumbnailData = artifactThumbnailRepository.getThumbnailData(artifactVertex.getId(), "poster-frame", boundaryDims[0], boundaryDims[1], user);
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: %s (poster-frame) %d x %d", graphVertexId, boundaryDims[0], boundaryDims[1]);
                ServletOutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
                return;
            }
        }

        StreamingPropertyValue rawPosterFrameValue = RAW_POSTER_FRAME.getPropertyValue(artifactVertex);
        if (rawPosterFrameValue == null) {
            LOGGER.warn("Could not find raw poster from for artifact: %s", artifactVertex.getId());
            respondWithNotFound(response);
            return;
        }

        InputStream in = rawPosterFrameValue.getInputStream();
        try {
            if (widthStr != null) {
                LOGGER.info("Cache miss for: %s (poster-frame) %d x %d", graphVertexId, boundaryDims[0], boundaryDims[1]);

                response.setContentType("image/jpeg");
                response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + ".jpg");
                setMaxAge(response, EXPIRES_1_HOUR);

                byte[] thumbnailData = artifactThumbnailRepository.createThumbnail(artifactVertex, "poster-frame", in, boundaryDims, user).getThumbnailData();
                ServletOutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
            } else {
                response.setContentType("image/png");
                IOUtils.copy(in, response.getOutputStream());
            }
        } finally {
            in.close();
        }
    }
}
