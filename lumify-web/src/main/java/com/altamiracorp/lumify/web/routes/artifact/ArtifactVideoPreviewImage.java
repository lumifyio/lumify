package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

import static com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties.VIDEO_PREVIEW_IMAGE;

public class ArtifactVideoPreviewImage extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactVideoPreviewImage.class);
    private final Graph graph;
    private final ArtifactThumbnailRepository artifactThumbnailRepository;
    private final UserRepository userRepository;

    @Inject
    public ArtifactVideoPreviewImage(
            final Graph graph,
            final ArtifactThumbnailRepository artifactThumbnailRepository,
            final UserRepository userRepository) {
        this.graph = graph;
        this.artifactThumbnailRepository = artifactThumbnailRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            chain.next(request, response);
            return;
        }

        String widthStr = getOptionalParameter(request, "width");
        int[] boundaryDims = new int[]{200 * ArtifactThumbnailRepository.FRAMES_PER_PREVIEW, 200};

        if (widthStr != null) {
            boundaryDims[0] = Integer.parseInt(widthStr) * ArtifactThumbnailRepository.FRAMES_PER_PREVIEW;
            boundaryDims[1] = Integer.parseInt(widthStr);

            response.setContentType("image/jpeg");
            response.addHeader("Content-Disposition", "inline; filename=thumnail" + boundaryDims[0] + ".jpg");

            byte[] thumbnailData = artifactThumbnailRepository.getThumbnailData(artifactVertex.getId(), "video-preview", boundaryDims[0],
                    boundaryDims[1], user);
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: %s (video-preview) %d x %d", artifactVertex.getId().toString(), boundaryDims[0], boundaryDims[1]);
                ServletOutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
                return;
            }
        }

        StreamingPropertyValue videoPreviewImageValue = VIDEO_PREVIEW_IMAGE.getPropertyValue(artifactVertex);
        if (videoPreviewImageValue == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            chain.next(request, response);
            return;
        }
        InputStream in = videoPreviewImageValue.getInputStream();
        try {
            if (widthStr != null) {
                LOGGER.info("Cache miss for: %s (video-preview) %d x %d", artifactVertex.getId().toString(), boundaryDims[0], boundaryDims[1]);
                byte[] thumbnailData = artifactThumbnailRepository.createThumbnail(artifactVertex.getId(), "video-preview", in,
                        boundaryDims, user).getMetadata().getData();
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
