package io.lumify.gpw.video;

import com.google.common.io.Files;
import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.video.VideoFrameInfo;
import io.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.properties.types.IntegerLumifyProperty;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import io.lumify.gpw.util.FFprobeRotationUtil;
import org.apache.commons.io.FileUtils;
import org.securegraph.*;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;

public class VideoFrameExtractGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VideoFrameExtractGraphPropertyWorker.class);
    private ProcessRunner processRunner;
    private IntegerLumifyProperty videoRotationProperty;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        getAuthorizationRepository().addAuthorizationToGraph(VideoFrameInfo.VISIBILITY);
        videoRotationProperty = new IntegerLumifyProperty(getOntologyRepository().getRequiredPropertyIRIByIntent("media.clockwiseRotation"));
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Integer videoRotation = videoRotationProperty.getPropertyValue(data.getElement(), 0);
        Visibility newVisibility = new LumifyVisibility(LumifyVisibility.and(getVisibilityTranslator().toVisibilityNoSuperUser(data.getVisibilityJson()), VideoFrameInfo.VISIBILITY)).getVisibility();

        Pattern fileNamePattern = Pattern.compile("image-([0-9]+)\\.png");
        File tempDir = Files.createTempDir();
        try {
            Double defaultFPSToExtract = 1.0;
            extractFrames(data.getLocalFile(), tempDir, data, defaultFPSToExtract, videoRotation);

            List<String> propertyKeys = new ArrayList<String>();
            long videoDuration = 0;
            for (File frameFile : tempDir.listFiles()) {
                Matcher m = fileNamePattern.matcher(frameFile.getName());
                if (!m.matches()) {
                    continue;
                }
                long frameStartTime = (long) ((Double.parseDouble(m.group(1)) / defaultFPSToExtract) * 1000.0);
                if (frameStartTime > videoDuration) {
                    videoDuration = frameStartTime;
                }

                try (InputStream frameFileIn = new FileInputStream(frameFile)) {
                    ExistingElementMutation<Vertex> mutation = data.getElement().prepareMutation();
                    StreamingPropertyValue frameValue = new StreamingPropertyValue(frameFileIn, byte[].class);
                    frameValue.searchIndex(false);
                    String key = String.format("%08d", Math.max(0L, frameStartTime));
                    Metadata metadata = data.createPropertyMetadata();
                    metadata.add(LumifyProperties.MIME_TYPE.getPropertyName(), "image/png", getVisibilityTranslator().getDefaultVisibility());
                    metadata.add(MediaLumifyProperties.METADATA_VIDEO_FRAME_START_TIME, frameStartTime, getVisibilityTranslator().getDefaultVisibility());

                    MediaLumifyProperties.VIDEO_FRAME.addPropertyValue(mutation, key, frameValue, metadata, newVisibility);
                    propertyKeys.add(key);
                    mutation.save(getAuthorizations());
                }
            }

            getGraph().flush();

            generateAndSaveVideoPreviewImage((Vertex) data.getElement(), videoRotation);

            for (String propertyKey : propertyKeys) {
                getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), propertyKey, MediaLumifyProperties.VIDEO_FRAME.getPropertyName());
            }
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private void extractFrames(File videoFileName, File outDir, GraphPropertyWorkData data, double framesPerSecondToExtract, int videoRotation) throws IOException, InterruptedException {
        String[] ffmpegOptionsArray = prepareFFMPEGOptions(videoFileName, outDir, data, framesPerSecondToExtract, videoRotation);
        processRunner.execute(
                "ffmpeg",
                ffmpegOptionsArray,
                null,
                videoFileName.getAbsolutePath() + ": "
        );
    }

    private String[] prepareFFMPEGOptions(File videoFileName, File outDir, GraphPropertyWorkData data, double framesPerSecondToExtract, int videoRotation) {

        ArrayList<String> ffmpegOptionsList = new ArrayList<>();
        ffmpegOptionsList.add("-i");
        ffmpegOptionsList.add(videoFileName.getAbsolutePath());
        ffmpegOptionsList.add("-r");
        ffmpegOptionsList.add("" + framesPerSecondToExtract);

        //Scale.
        //Will not force conversion to 720:480 aspect ratio, but will resize video with original aspect ratio.
        if (videoRotation == 0 || videoRotation == 180) {
            ffmpegOptionsList.add("-s");
            ffmpegOptionsList.add("720x480");
        } else if (videoRotation == 90 || videoRotation == 270) {
            ffmpegOptionsList.add("-s");
            ffmpegOptionsList.add("480x720");
        }

        //Rotate.
        String[] ffmpegRotationOptions = FFprobeRotationUtil.createFFMPEGRotationOptions(videoRotation);
        if (ffmpegRotationOptions != null) {
            ffmpegOptionsList.add(ffmpegRotationOptions[0]);
            ffmpegOptionsList.add(ffmpegRotationOptions[1]);
        }

        ffmpegOptionsList.add(new File(outDir, "image-%8d.png").getAbsolutePath());
        return ffmpegOptionsList.toArray(new String[ffmpegOptionsList.size()]);
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }
        String mimeType = LumifyProperties.MIME_TYPE.getMetadataValue(property.getMetadata(), null);
        if (mimeType == null || !mimeType.startsWith("video")) {
            return false;
        }

        return true;
    }

    private void generateAndSaveVideoPreviewImage(Vertex artifactVertex, int videoRotation) {
        LOGGER.info("Generating video preview for %s", artifactVertex.getId());

        try {
            Iterable<Property> videoFrames = getVideoFrameProperties(artifactVertex);
            List<Property> videoFramesForPreview = getFramesForPreview(videoFrames);
            BufferedImage previewImage = createPreviewImage(videoFramesForPreview, videoRotation);
            saveImage(artifactVertex, previewImage);
        } catch (IOException e) {
            throw new RuntimeException("Could not create preview image for artifact: " + artifactVertex.getId(), e);
        }

        LOGGER.debug("Finished creating preview for: %s", artifactVertex.getId());
    }

    private void saveImage(Vertex artifactVertex, BufferedImage previewImage) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(previewImage, "png", out);
        StreamingPropertyValue spv = new StreamingPropertyValue(new ByteArrayInputStream(out.toByteArray()), byte[].class);
        spv.searchIndex(false);
        MediaLumifyProperties.VIDEO_PREVIEW_IMAGE.setProperty(artifactVertex, spv, artifactVertex.getVisibility(), getAuthorizations());
        getGraph().flush();
    }

    private BufferedImage createPreviewImage(List<Property> videoFrames, int videoRotation) throws IOException {
        int previewFrameWidth;
        int previewFrameHeight;
        if (videoRotation == 0 || videoRotation == 180) {
            previewFrameWidth = ArtifactThumbnailRepository.PREVIEW_FRAME_WIDTH;
            previewFrameHeight = ArtifactThumbnailRepository.PREVIEW_FRAME_HEIGHT;
        } else {
            previewFrameWidth = ArtifactThumbnailRepository.PREVIEW_FRAME_HEIGHT;
            previewFrameHeight = ArtifactThumbnailRepository.PREVIEW_FRAME_WIDTH;
        }

        BufferedImage previewImage = new BufferedImage(previewFrameWidth * videoFrames.size(), previewFrameHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = previewImage.getGraphics();
        for (int i = 0; i < videoFrames.size(); i++) {
            Property videoFrame = videoFrames.get(i);
            Image img = loadImage(videoFrame);
            int dx1 = i * previewFrameWidth;
            int dy1 = 0;
            int dx2 = dx1 + previewFrameWidth;
            int dy2 = previewFrameHeight;
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
        try (InputStream spvIn = spv.getInputStream()) {
            BufferedImage img = ImageIO.read(spvIn);
            checkNotNull(img, "Could not load image from frame: " + videoFrame);
            return img;
        }
    }

    private Iterable<Property> getVideoFrameProperties(Vertex artifactVertex) {
        List<Property> videoFrameProperties = toList(artifactVertex.getProperties(MediaLumifyProperties.VIDEO_FRAME.getPropertyName()));
        Collections.sort(videoFrameProperties, new Comparator<Property>() {
            @Override
            public int compare(Property p1, Property p2) {
                Long p1StartTime = (Long) p1.getMetadata().getValue(MediaLumifyProperties.METADATA_VIDEO_FRAME_START_TIME);
                Long p2StartTime = (Long) p2.getMetadata().getValue(MediaLumifyProperties.METADATA_VIDEO_FRAME_START_TIME);
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

    @Inject
    public void setProcessRunner(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }
}
