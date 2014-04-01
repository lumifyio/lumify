package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties.VIDEO_PREVIEW_IMAGE;
import static com.altamiracorp.securegraph.util.IterableUtils.toList;
import static com.google.common.base.Preconditions.checkNotNull;

public class ArtifactVideoPreviewImage extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactVideoPreviewImage.class);
    private final Graph graph;
    private final ArtifactThumbnailRepository artifactThumbnailRepository;

    @Inject
    public ArtifactVideoPreviewImage(
            final Graph graph,
            final ArtifactThumbnailRepository artifactThumbnailRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
        this.artifactThumbnailRepository = artifactThumbnailRepository;
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
            generateAndSaveVideoPreviewImage(artifactVertex);
            videoPreviewImageValue = VIDEO_PREVIEW_IMAGE.getPropertyValue(artifactVertex);
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

    private void generateAndSaveVideoPreviewImage(Vertex artifactVertex) {
        LOGGER.info("Generating video preview for %s", artifactVertex.getId().toString());

        try {
            Iterable<Property> videoFrames = getVideoFrameProperties(artifactVertex);
            List<Property> videoFramesForPreview = getFramesForPreview(videoFrames);
            BufferedImage previewImage = createPreviewImage(videoFramesForPreview);
            saveImage(artifactVertex, previewImage);
        } catch (IOException e) {
            throw new RuntimeException("Could not create preview image for artifact: " + artifactVertex.getId(), e);
        }

        LOGGER.debug("Finished creating preview for: %s", artifactVertex.getId().toString());
    }

    private void saveImage(Vertex artifactVertex, BufferedImage previewImage) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(previewImage, "png", out);
        StreamingPropertyValue spv = new StreamingPropertyValue(new ByteArrayInputStream(out.toByteArray()), byte[].class);
        spv.searchIndex(false);
        MediaLumifyProperties.VIDEO_PREVIEW_IMAGE.setProperty(artifactVertex, spv, artifactVertex.getVisibility());
        graph.flush();
    }

    private BufferedImage createPreviewImage(List<Property> videoFrames) throws IOException {
        BufferedImage previewImage = new BufferedImage(ArtifactThumbnailRepository.PREVIEW_FRAME_WIDTH * videoFrames.size(), ArtifactThumbnailRepository.PREVIEW_FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics g = previewImage.getGraphics();
        for (int i = 0; i < videoFrames.size(); i++) {
            Property videoFrame = videoFrames.get(i);
            Image img = loadImage(videoFrame);
            int dx1 = i * ArtifactThumbnailRepository.PREVIEW_FRAME_WIDTH;
            int dy1 = 0;
            int dx2 = dx1 + ArtifactThumbnailRepository.PREVIEW_FRAME_WIDTH;
            int dy2 = ArtifactThumbnailRepository.PREVIEW_FRAME_HEIGHT;
            int sx1 = 0;
            int sy1 = 0;
            int sx2 = img.getWidth(null);
            int sy2 = img.getHeight(null);
            g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
        }
        return previewImage;
    }

    private Image loadImage(Property videoFrame) throws IOException {
        StreamingPropertyValue spv = (StreamingPropertyValue) videoFrame.getValue();
        InputStream spvIn = spv.getInputStream();
        try {
            BufferedImage img = ImageIO.read(spvIn);
            checkNotNull(img, "Could not load image from frame: " + videoFrame);
            return img;
        } finally {
            spvIn.close();
        }
    }

    private Iterable<Property> getVideoFrameProperties(Vertex artifactVertex) {
        List<Property> videoFrameProperties = toList(artifactVertex.getProperties(MediaLumifyProperties.VIDEO_FRAME.getKey()));
        Collections.sort(videoFrameProperties, new Comparator<Property>() {
            @Override
            public int compare(Property p1, Property p2) {
                Long p1StartTime = (Long) p1.getMetadata().get(MediaLumifyProperties.METADATA_VIDEO_FRAME_START_TIME);
                Long p2StartTime = (Long) p2.getMetadata().get(MediaLumifyProperties.METADATA_VIDEO_FRAME_START_TIME);
                return p1StartTime.compareTo(p2StartTime);
            }
        });
        return videoFrameProperties;
    }

    private List<Property> getFramesForPreview(Iterable<Property> videoFramesIterable) {
        List<Property> videoFrames = toList(videoFramesIterable);
        ArrayList<Property> results = new ArrayList<Property>();
        double skip = (double) videoFrames.size() / (double) ArtifactThumbnailRepository.FRAMES_PER_PREVIEW;
        for (double i = 0; i < videoFrames.size(); i += skip) {
            results.add(videoFrames.get((int) Math.floor(i)));
        }
        if (results.size() < 20) {
            results.add(videoFrames.get(videoFrames.size() - 1));
        }
        if (results.size() > 20) {
            results.remove(results.size() - 1);
        }
        return results;
    }

}
