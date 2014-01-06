package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRowKey;
import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

public class ArtifactPosterFrame extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactPosterFrame.class);

    private final ArtifactRepository artifactRepository;
    private final ArtifactThumbnailRepository artifactThumbnailRepository;

    @Inject
    public ArtifactPosterFrame(final ArtifactRepository artifactRepo, final ArtifactThumbnailRepository thumbnailRepo) {
        artifactRepository = artifactRepo;
        artifactThumbnailRepository = thumbnailRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));

        String widthStr = getOptionalParameter(request, "width");
        int[] boundaryDims = new int[]{200, 200};

        ArtifactRowKey artifactRowKey = artifactRepository.findRowKeyByGraphVertexId(graphVertexId, user);
        if (artifactRowKey == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            chain.next(request, response);
            return;
        }

        if (widthStr != null) {
            boundaryDims[0] = boundaryDims[1] = Integer.parseInt(widthStr);

            response.setContentType("image/jpeg");
            response.addHeader("Content-Disposition", "inline; filename=thumnail" + boundaryDims[0] + ".jpg");

            byte[] thumbnailData = artifactThumbnailRepository.getThumbnailData(artifactRowKey, "poster-frame", boundaryDims[0], boundaryDims[1], user);
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: %s (poster-frame) %d x %d", graphVertexId, boundaryDims[0], boundaryDims[1]);
                ServletOutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
                return;
            }
        }

        InputStream in = artifactRepository.getRawPosterFrame(artifactRowKey.toString());
        try {
            if (widthStr != null) {
                LOGGER.info("Cache miss for: %s (poster-frame) %d x %d", graphVertexId, boundaryDims[0], boundaryDims[1]);
                byte[] thumbnailData = artifactThumbnailRepository.createThumbnail(artifactRowKey, "poster-frame", in, boundaryDims, user).getMetadata().getData();
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
