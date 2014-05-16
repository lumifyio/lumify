package io.lumify.storm.video;

import com.google.common.io.Files;
import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.util.ProcessRunner;
import org.apache.commons.io.FileUtils;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoFrameExtractGraphPropertyWorker extends GraphPropertyWorker {
    private ProcessRunner processRunner;
    private double framesPerSecondToExtract = 0.1; // TODO make this configurable

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Pattern fileNamePattern = Pattern.compile("image-([0-9]+)\\.png");
        File tempDir = Files.createTempDir();
        try {
            extractFrames(data.getLocalFile(), tempDir, framesPerSecondToExtract);

            List<String> propertyKeys = new ArrayList<String>();
            long videoDuration = 0;
            for (File frameFile : tempDir.listFiles()) {
                Matcher m = fileNamePattern.matcher(frameFile.getName());
                if (!m.matches()) {
                    continue;
                }
                long frameStartTime = (long) ((Double.parseDouble(m.group(1)) / framesPerSecondToExtract) * 1000.0);
                if (frameStartTime > videoDuration) {
                    videoDuration = frameStartTime;
                }

                InputStream frameFileIn = new FileInputStream(frameFile);
                try {
                    ExistingElementMutation<Vertex> mutation = data.getElement().prepareMutation();
                    StreamingPropertyValue frameValue = new StreamingPropertyValue(frameFileIn, byte[].class);
                    frameValue.searchIndex(false);
                    String key = String.format("%08d", frameStartTime);
                    Map<String, Object> metadata = data.getPropertyMetadata();
                    metadata.put(RawLumifyProperties.MIME_TYPE.getKey(), "image/png");
                    metadata.put(MediaLumifyProperties.METADATA_VIDEO_FRAME_START_TIME, frameStartTime);
                    MediaLumifyProperties.VIDEO_FRAME.addPropertyValue(mutation, key, frameValue, metadata, data.getVisibility());
                    propertyKeys.add(key);
                    mutation.save();
                } finally {
                    frameFileIn.close();
                }
            }

            getGraph().flush();

            for (String propertyKey : propertyKeys) {
                getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), propertyKey, MediaLumifyProperties.VIDEO_FRAME.getKey());
            }
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private void extractFrames(File videoFileName, File outDir, double framesPerSecondToExtract) throws IOException, InterruptedException {
        processRunner.execute(
                "ffmpeg",
                new String[]{
                        "-i", videoFileName.getAbsolutePath(),
                        "-r", "" + framesPerSecondToExtract,
                        new File(outDir, "image-%8d.png").getAbsolutePath()
                },
                null,
                videoFileName.getAbsolutePath() + ": "
        );
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }
        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.MIME_TYPE.getKey());
        if (mimeType == null || !mimeType.startsWith("video")) {
            return false;
        }

        return true;
    }


    @Inject
    public void setProcessRunner(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }
}
