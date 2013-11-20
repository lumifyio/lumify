package com.altamiracorp.lumify.web.routes.artifact;

import java.io.InputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRowKey;
import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnail;
import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.google.inject.Inject;

public class ArtifactThumbnailByRowKey extends BaseRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactThumbnailByRowKey.class);

    private final ArtifactRepository artifactRepository;
    private final ArtifactThumbnailRepository artifactThumbnailRepository;
    private final GraphRepository graphRepository;

    @Inject
    public ArtifactThumbnailByRowKey(final ArtifactRepository artifactRepo,
                                     final ArtifactThumbnailRepository thumbnailRepo,
                                     final GraphRepository graphRepository) {
        artifactRepository = artifactRepo;
        artifactThumbnailRepository = thumbnailRepo;
        this.graphRepository = graphRepository;
    }

    public static String getUrl(ArtifactRowKey artifactKey) {
        return "/artifact/" + UrlUtils.urlEncode(artifactKey.toString()) + "/thumbnail";
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        ArtifactRowKey artifactRowKey = new ArtifactRowKey(UrlUtils.urlDecode(getAttributeString(request, "_rowKey")));

        String widthStr = getOptionalParameter(request, "width");
        int[] boundaryDims = new int[]{200, 200};
        if (widthStr != null) {
            boundaryDims[0] = boundaryDims[1] = Integer.parseInt(widthStr);
        }

        byte[] thumbnailData;
        ArtifactThumbnail thumbnail = artifactThumbnailRepository.getThumbnail(artifactRowKey, "raw", boundaryDims[0], boundaryDims[1], user);
        if (thumbnail != null) {
            String format = thumbnail.getMetadata().getFormat();
            response.setContentType("image/" + format);
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + "." + format);

            thumbnailData = thumbnail.getMetadata().getData();
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: " + artifactRowKey.toString() + " (raw) " + boundaryDims[0] + "x" + boundaryDims[1]);
                ServletOutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
                return;
            }
        }

        Artifact artifact = artifactRepository.findByRowKey(artifactRowKey.toString(), user.getModelUserContext());
        if (artifact == null) {
            LOGGER.warn("Cannot find artifact with row key: " + artifactRowKey.toString());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            chain.next(request, response);
            return;
        }

        GraphVertex vertex = graphRepository.findVertex(artifact.getMetadata().getGraphVertexId(), user);

        LOGGER.info("Cache miss for: " + artifactRowKey.toString() + " (raw) " + boundaryDims[0] + "x" + boundaryDims[1]);
        InputStream in = artifactRepository.getRaw(artifact, vertex, user);
        try {
            thumbnail = artifactThumbnailRepository.createThumbnail(artifact.getRowKey(), "raw", in, boundaryDims, user);

            String format = thumbnail.getMetadata().getFormat();
            response.setContentType("image/" + format);
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + "." + format);

            thumbnailData = thumbnail.getMetadata().getData();
        } finally {
            in.close();
        }
        ServletOutputStream out = response.getOutputStream();
        out.write(thumbnailData);
        out.close();
    }
}
