package io.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Vertex;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ArtifactThumbnailRepository extends Repository<BigTableArtifactThumbnail> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactThumbnailRepository.class);
    public static int FRAMES_PER_PREVIEW = 20;
    public static int PREVIEW_FRAME_WIDTH = 360;
    public static int PREVIEW_FRAME_HEIGHT = 240;
    private final OntologyProperty yAxisFlippedIri;
    private final OntologyProperty clockwiseRotationIri;

    public ArtifactThumbnailRepository(ModelSession modelSession, final OntologyRepository ontologyRepository) {
        super(modelSession);

        this.yAxisFlippedIri = ontologyRepository.getPropertyByIntent("media.yAxisFlipped");
        this.clockwiseRotationIri = ontologyRepository.getPropertyByIntent("media.clockwiseRotation");
    }

    public abstract BigTableArtifactThumbnail fromRow(Row row);

    public abstract Row toRow(BigTableArtifactThumbnail artifactThumbnail);

    public abstract String getTableName();

    public abstract ArtifactThumbnail getThumbnail(Object artifactVertexId, String thumbnailType, int width, int height, User user);

    public abstract byte[] getThumbnailData(Object artifactVertexId, String thumbnailType, int width, int height, User user);

    public abstract ArtifactThumbnail createThumbnail(Vertex artifactVertex, String thumbnailType, InputStream in, int[] boundaryDims, User user) throws IOException;

    public ArtifactThumbnail generateThumbnail(Vertex artifactVertex, InputStream in, int[] boundaryDims) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String format;
        int type;
        try {
            BufferedImage originalImage = ImageIO.read(in);
            checkNotNull(originalImage, "Could not generateThumbnail: read original image for artifact " + artifactVertex.getId());
            type = ImageUtils.thumbnailType(originalImage);
            format = ImageUtils.thumbnailFormat(originalImage);

            BufferedImage transformedImage = getTransformedImage(originalImage, artifactVertex);

            //Get new image dimensions, which will be used for the icon.
            int[] transformedImageDims = new int[]{transformedImage.getWidth(), transformedImage.getHeight()};
            int[] newImageDims = getScaledDimension(transformedImageDims, boundaryDims);
            if (newImageDims[0] >= transformedImageDims[0] || newImageDims[1] >= transformedImageDims[1]) {
                LOGGER.info("Original image dimensions %d x %d are smaller "
                                + "than requested dimensions %d x %d returning original.",
                        transformedImageDims[0], transformedImageDims[1],
                        newImageDims[0], newImageDims[1]);
            }
            //Resize the image.
            BufferedImage resizedImage = new BufferedImage(newImageDims[0], newImageDims[1], type);
            Graphics2D g = resizedImage.createGraphics();
            if (transformedImage.getColorModel().getNumComponents() > 3) {
                g.drawImage(transformedImage, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), null);
            } else {
                g.drawImage(transformedImage, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), Color.BLACK, null);
            }
            g.dispose();

            //Write the bufferedImage to a file.
            ImageIO.write(resizedImage, format, out);
        } catch (IOException e) {
            throw new LumifyResourceNotFoundException("Error reading inputstream");
        }
        return new ArtifactThumbnail(out.toByteArray(), type, format);
    }

    public BufferedImage getTransformedImage(BufferedImage originalImage, Vertex artifactVertex) {
        int cwRotationNeeded = 0;
        if (clockwiseRotationIri != null) {
            Integer nullable = (Integer) artifactVertex.getPropertyValue(clockwiseRotationIri.getTitle());
            if (nullable != null) {
                cwRotationNeeded = nullable;
            }
        }
        boolean yAxisFlipNeeded = false;
        if (yAxisFlippedIri != null) {
            Boolean nullable = (Boolean) artifactVertex.getPropertyValue(yAxisFlippedIri.getTitle());
            if (nullable != null) {
                yAxisFlipNeeded = nullable;
            }
        }

        //Rotate and flip image.
        return ImageUtils.reOrientImage(originalImage, yAxisFlipNeeded, cwRotationNeeded);
    }

    public int[] getScaledDimension(int[] imgSize, int[] boundary) {
        int originalWidth = imgSize[0];
        int originalHeight = imgSize[1];
        int boundWidth = boundary[0];
        int boundHeight = boundary[1];
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > boundWidth) {
            newWidth = boundWidth;
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        if (newHeight > boundHeight) {
            newHeight = boundHeight;
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        return new int[]{newWidth, newHeight};
    }
}
