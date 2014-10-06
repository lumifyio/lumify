package io.lumify.web.routes.resource;

import io.lumify.miniweb.HandlerChain;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;

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
import java.util.concurrent.TimeUnit;

public class MapMarkerImage extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MapMarkerImage.class);

    private final OntologyRepository ontologyRepository;
    private ArtifactThumbnailRepository artifactThumbnailRepository;
    private final Cache<String, byte[]> imageCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Inject
    public MapMarkerImage(
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            final ArtifactThumbnailRepository artifactThumbnailRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.artifactThumbnailRepository = artifactThumbnailRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String typeStr = getAttributeString(request, "type");
        long scale = getOptionalParameterLong(request, "scale", 1L);
        int heading = roundHeadingAngle(getOptionalParameterDouble(request, "heading", 0.0));
        boolean selected = getOptionalParameter(request, "selected") != null;

        String cacheKey = typeStr + scale + heading + (selected ? "selected" : "unselected");
        byte[] imageData = imageCache.getIfPresent(cacheKey);
        if (imageData == null) {
            LOGGER.info("map marker cache miss %s (scale: %d, heading: %d)", typeStr, scale, heading);

            Concept concept = ontologyRepository.getConceptByIRI(typeStr);
            if (concept == null) {
                concept = ontologyRepository.getConceptByIRI(typeStr);
            }

            boolean isMapGlyphIcon = false;
            byte[] glyphIcon = getMapGlyphIcon(concept, user);
            if (glyphIcon != null) {
                isMapGlyphIcon = true;
            } else {
                glyphIcon = getGlyphIcon(concept, user);
                if (glyphIcon == null) {
                    respondWithNotFound(response);
                    return;
                }
            }

            imageData = getMarkerImage(new ByteArrayInputStream(glyphIcon), scale, selected, heading, isMapGlyphIcon);
            imageCache.put(cacheKey, imageData);
        }

        response.setHeader("Cache-Control", "max-age=" + (5 * 60));
        ServletOutputStream out = response.getOutputStream();
        out.write(imageData);
        out.close();
    }

    private int roundHeadingAngle(double heading) {
        while (heading < 0.0) {
            heading += 360.0;
        }
        while (heading > 360.0) {
            heading -= 360.0;
        }
        return (int) (Math.round(heading / 10.0) * 10.0);
    }

    private byte[] getMarkerImage(InputStream resource, long scale, boolean selected, int heading, boolean isMapGlyphIcon) throws IOException {
        BufferedImage resourceImage = ImageIO.read(resource);
        if (resourceImage == null) {
            return null;
        }

        if (heading != 0) {
            resourceImage = rotateImage(resourceImage, heading);
        }

        BufferedImage backgroundImage = getBackgroundImage(scale, selected);
        if (backgroundImage == null) {
            return null;
        }
        int[] resourceImageDim = new int[]{resourceImage.getWidth(), resourceImage.getHeight()};

        BufferedImage image = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), backgroundImage.getType());
        Graphics2D g = image.createGraphics();
        if (isMapGlyphIcon) {
            int[] boundary = new int[]{backgroundImage.getWidth(), backgroundImage.getHeight()};
            int[] scaledDims = artifactThumbnailRepository.getScaledDimension(resourceImageDim, boundary);
            g.drawImage(resourceImage, 0, 0, scaledDims[0], scaledDims[1], null);
        } else {
            g.drawImage(backgroundImage, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), null);
            int size = image.getWidth() * 2 / 3;
            int[] boundary = new int[]{size, size};
            int[] scaledDims = artifactThumbnailRepository.getScaledDimension(resourceImageDim, boundary);
            int x = (backgroundImage.getWidth() - scaledDims[0]) / 2;
            int y = (backgroundImage.getWidth() - scaledDims[1]) / 2;
            g.drawImage(resourceImage, x, y, scaledDims[0], scaledDims[1], null);
        }
        g.dispose();
        return imageToBytes(image);
    }

    private BufferedImage rotateImage(BufferedImage image, int angleDeg) {
        BufferedImage rotatedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g = rotatedImage.createGraphics();
        g.rotate(Math.toRadians(angleDeg), rotatedImage.getWidth() / 2, rotatedImage.getHeight() / 2);
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return rotatedImage;
    }

    private BufferedImage getBackgroundImage(long scale, boolean selected) throws IOException {
        InputStream res;
        if (scale == 1) {
            res = this.getClass().getResourceAsStream(selected ? "marker-background-selected.png" : "marker-background.png");
        } else if (scale == 2) {
            res = this.getClass().getResourceAsStream(selected ? "marker-background-selected-2x.png" : "marker-background-2x.png");
        } else {
            return null;
        }
        return ImageIO.read(res);
    }

    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream imageData = new ByteArrayOutputStream();
        ImageIO.write(image, "png", imageData);
        imageData.close();
        return imageData.toByteArray();
    }

    private byte[] getMapGlyphIcon(Concept concept, User user) {
        byte[] mapGlyphIcon = null;
        for (Concept con = concept; mapGlyphIcon == null && con != null; con = ontologyRepository.getParentConcept(con)) {
            mapGlyphIcon = con.getMapGlyphIcon();
        }
        return mapGlyphIcon;
    }

    private byte[] getGlyphIcon(Concept concept, User user) {
        byte [] glyphIcon = null;
        for (Concept con = concept; glyphIcon == null && con != null; con = ontologyRepository.getParentConcept(con)) {
            glyphIcon = con.hasGlyphIconResource() ? con.getGlyphIcon() : null;
        }
        return glyphIcon;
    }
}
