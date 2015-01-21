package io.lumify.analystsNotebook;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.codec.binary.Base64;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class AnalystsNotebookImageUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AnalystsNotebookImageUtil.class);
    public static final String DEFAULT_FORMAT_NAME = "bmp";
    public static final Integer DEFAULT_INTERMEDIATE_BUFFERED_IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;
    public static final Color DEFAULT_INTERMEDIATE_BACKGROUND_COLOR = Color.WHITE;

    public static String base64EncodedImageBytes(byte[] data) {
        return Base64.encodeBase64String(data);
    }

    public static byte[] convertImageFormat(byte[] data) {
        return convertImageFormat(data, DEFAULT_FORMAT_NAME, DEFAULT_INTERMEDIATE_BUFFERED_IMAGE_TYPE, DEFAULT_INTERMEDIATE_BACKGROUND_COLOR);
    }

    public static byte[] convertImageFormat(byte[] data, String formatName, Integer intermediateBufferedImageType, Color intermediateBackgroundColor) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            BufferedImage input = ImageIO.read(new ByteArrayInputStream(data));
            input = intermediateConversion(input, intermediateBufferedImageType, intermediateBackgroundColor);
            boolean written = ImageIO.write(input, formatName, output);
            if (written) {
                if (LOGGER.isDebugEnabled()) {
                    File tempFile = File.createTempFile(AnalystsNotebookImageUtil.class.getSimpleName() + "-", "." + formatName);
                    tempFile.deleteOnExit();
                    ImageIO.write(input, formatName, tempFile);
                    LOGGER.debug("converted image written to " + tempFile.getCanonicalPath() + " for inspection and will be deleted on JVM exit");
                }
            } else {
                throw new LumifyException("no appropriate writer found for format: " + formatName);
            }
        } catch (IOException e) {
            throw new LumifyException("IOException converting image format", e);
        }
        return output.toByteArray();
    }

    private static BufferedImage intermediateConversion(BufferedImage input, Integer bufferedImageType, Color backgroundColor) {
        if (bufferedImageType == null) {
            return input;
        }
        BufferedImage intermediate = new BufferedImage(input.getWidth(), input.getHeight(), bufferedImageType);
        Graphics graphics = intermediate.getGraphics();
        if (backgroundColor == null) {
            graphics.drawImage(input, 0, 0, null);
            LOGGER.debug("converted image to intermediate type: %d", bufferedImageType);
        } else {
            graphics.drawImage(input, 0, 0, backgroundColor, null);
            LOGGER.debug("converted image to intermediate type: %d, with background color: %s", bufferedImageType, backgroundColor.toString());
        }
        return intermediate;
    }
}
