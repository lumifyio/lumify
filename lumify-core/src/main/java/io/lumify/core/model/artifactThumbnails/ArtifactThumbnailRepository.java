package io.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.*;
import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class ArtifactThumbnailRepository extends Repository<BigTableArtifactThumbnail> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactThumbnailRepository.class);
    public static int FRAMES_PER_PREVIEW = 20;
    public static int PREVIEW_FRAME_WIDTH = 360;
    public static int PREVIEW_FRAME_HEIGHT = 240;

    public ArtifactThumbnailRepository(ModelSession modelSession) {
        super(modelSession);
    }

    public abstract BigTableArtifactThumbnail fromRow(Row row);

    public abstract Row toRow(BigTableArtifactThumbnail artifactThumbnail);

    public abstract String getTableName();

    public abstract ArtifactThumbnail getThumbnail(Object artifactVertexId, String thumbnailType, int width, int height, User user);

    public abstract byte[] getThumbnailData(Object artifactVertexId, String thumbnailType, int width, int height, User user);

    public abstract ArtifactThumbnail createThumbnail(Object artifactVertexId, String thumbnailType, InputStream in, int[] boundaryDims, User user) throws IOException;

    public ArtifactThumbnail generateThumbnail (InputStream in, int[] boundaryDims) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String format;
        int type;
        try {
            BufferedImage originalImage = ImageIO.read(in);
            int[] originalImageDims = new int[]{originalImage.getWidth(), originalImage.getHeight()};
            int[] newImageDims = getScaledDimension(originalImageDims, boundaryDims);

            if (newImageDims[0] >= originalImageDims[0] || newImageDims[1] >= originalImageDims[1]) {
                LOGGER.info("Original image dimensions %d x %d are smaller "
                                + "than requested dimensions %d x %d returning original.",
                        originalImageDims[0], originalImageDims[1],
                        newImageDims[0], newImageDims[1]);
            }

            type = thumnbailType(originalImage);
            format = thumbnailFormat(originalImage);

            BufferedImage resizedImage = new BufferedImage(newImageDims[0], newImageDims[1], type);
            Graphics2D g = resizedImage.createGraphics();
            if (originalImage.getColorModel().getNumComponents() > 3) {
                g.drawImage(originalImage, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), null);
            } else {
                g.drawImage(originalImage, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), Color.BLACK, null);
            }
            g.dispose();
            ImageIO.write(resizedImage, format, out);
        } catch (IOException e) {
            throw new LumifyResourceNotFoundException("Error reading inputstream");
        }
        return new ArtifactThumbnail(out.toByteArray(), type, format);
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

    private int thumnbailType(BufferedImage image) {
        if (image.getColorModel().getNumComponents() > 3) {
            return BufferedImage.TYPE_4BYTE_ABGR;
        }
        return BufferedImage.TYPE_INT_RGB;
    }

    private String thumbnailFormat(BufferedImage image) {
        if (image.getColorModel().getNumComponents() > 3) {
            return "png";
        }
        return "jpg";
    }
}