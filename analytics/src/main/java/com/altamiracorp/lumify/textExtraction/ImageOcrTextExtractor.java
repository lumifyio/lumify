package com.altamiracorp.lumify.textExtraction;

import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrameRepository;
import com.google.inject.Inject;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.vietocr.ImageHelper;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class ImageOcrTextExtractor {
    private static final String NAME = "imageOCRExtractor";
    private static final List<String> ICON_MIME_TYPES = Arrays.asList("image/x-icon", "image/vnd.microsoft.icon");
    private Tesseract tesseract;

    @Inject
    public ImageOcrTextExtractor() {
        tesseract = Tesseract.getInstance();
    }

    public ArtifactExtractedInfo extractFromImage(BufferedImage image, String mimeType) throws Exception {
        if (isIcon(mimeType)) {
            return null;
        }
        String ocrResults = extractTextFromImage(image);
        if (ocrResults == null) {
            return null;
        }
        ArtifactExtractedInfo extractedInfo = new ArtifactExtractedInfo();
        extractedInfo.setText(ocrResults);
        return extractedInfo;
    }

    public VideoFrameExtractedInfo extractFromVideoFrame(BufferedImage videoFrame, String mimeType) throws Exception {
        ArtifactExtractedInfo info = extractFromImage(videoFrame, mimeType);
        if (info == null) {
            return null;
        }
        VideoFrameExtractedInfo extractedInfo = new VideoFrameExtractedInfo();
        extractedInfo.setText(info.getText());
        return extractedInfo;
    }

    public String getName() {
        return NAME;
    }

    private String extractTextFromImage(BufferedImage image) throws TesseractException {
        BufferedImage grayImage = ImageHelper.convertImageToGrayscale(image);
        String ocrResults = tesseract.doOCR(grayImage);
        if (ocrResults == null || ocrResults.trim().length() == 0) {
            return null;
        }
        ocrResults = ocrResults.trim();
        // TODO remove the trash that doesn't seem to be words
        return ocrResults;
    }

    private boolean isIcon(String mimeType) {
        return ICON_MIME_TYPES.contains(mimeType);
    }
}
