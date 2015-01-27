package io.lumify.gpw.video;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.properties.types.IntegerLumifyProperty;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import io.lumify.gpw.util.FFprobeRotationUtil;
import org.securegraph.Element;
import org.securegraph.Metadata;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class VideoMp4EncodingWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VideoMp4EncodingWorker.class);
    private static final String PROPERTY_KEY = VideoMp4EncodingWorker.class.getName();
    private ProcessRunner processRunner;
    private IntegerLumifyProperty videoRotationProperty;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        videoRotationProperty = new IntegerLumifyProperty(getOntologyRepository().getRequiredPropertyIRIByIntent("media.clockwiseRotation"));
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp4File = File.createTempFile("encode_mp4_", ".mp4");
        File mp4RelocatedFile = File.createTempFile("relocated_mp4_", ".mp4");
        String[] ffmpegOptionsArray = prepareFFMPEGOptions(data, mp4File);
        try {
            processRunner.execute(
                    "ffmpeg",
                    ffmpegOptionsArray,
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            processRunner.execute(
                    "qt-faststart",
                    new String[]{
                            mp4File.getAbsolutePath(),
                            mp4RelocatedFile.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

            try (InputStream mp4RelocatedFileIn = new FileInputStream(mp4RelocatedFile)) {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4RelocatedFileIn, byte[].class);
                spv.searchIndex(false);
                Metadata metadata = new Metadata();
                metadata.add(LumifyProperties.MIME_TYPE.getPropertyName(), MediaLumifyProperties.MIME_TYPE_VIDEO_MP4, getVisibilityTranslator().getDefaultVisibility());
                MediaLumifyProperties.VIDEO_MP4.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
            }
        } finally {
            if (!mp4File.delete()) {
                LOGGER.warn("Could not delete %s" + mp4File.getAbsolutePath());
            }
            if (!mp4RelocatedFile.delete()) {
                LOGGER.warn("Could not delete %s" + mp4RelocatedFile.getAbsolutePath());
            }
        }
    }

    public String[] prepareFFMPEGOptions(GraphPropertyWorkData data, File mp4File) {
        ArrayList<String> ffmpegOptionsList = new ArrayList<>();

        ffmpegOptionsList.add("-y");
        ffmpegOptionsList.add("-i");
        ffmpegOptionsList.add(data.getLocalFile().getAbsolutePath());
        ffmpegOptionsList.add("-vcodec");
        ffmpegOptionsList.add("libx264");
        ffmpegOptionsList.add("-vprofile");
        ffmpegOptionsList.add("high");
        ffmpegOptionsList.add("-preset");
        ffmpegOptionsList.add("slow");
        ffmpegOptionsList.add("-b:v");
        ffmpegOptionsList.add("500k");
        ffmpegOptionsList.add("-maxrate");
        ffmpegOptionsList.add("500k");
        ffmpegOptionsList.add("-bufsize");
        ffmpegOptionsList.add("1000k");

        //Scale.
        //Will not force conversion to 720:480 aspect ratio, but will resize video with original aspect ratio.
        ffmpegOptionsList.add("-vf");
        ffmpegOptionsList.add("scale=720:480");

        Integer videoRotation = videoRotationProperty.getPropertyValue(data.getElement());
        if (videoRotation != null) {
            String[] ffmpegRotationOptions = FFprobeRotationUtil.createFFMPEGRotationOptions(videoRotation);
            //Rotate
            if (ffmpegRotationOptions != null) {
                ffmpegOptionsList.add(ffmpegRotationOptions[0]);
                ffmpegOptionsList.add(ffmpegRotationOptions[1]);
            }
        }

        ffmpegOptionsList.add("-threads");
        ffmpegOptionsList.add("0");
        ffmpegOptionsList.add("-acodec");
        ffmpegOptionsList.add("libfdk_aac");
        ffmpegOptionsList.add("-b:a");
        ffmpegOptionsList.add("128k");
        ffmpegOptionsList.add("-f");
        ffmpegOptionsList.add("mp4");
        ffmpegOptionsList.add(mp4File.getAbsolutePath());
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

        if (MediaLumifyProperties.VIDEO_MP4.hasProperty(element, PROPERTY_KEY)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }

    @Inject
    public void setProcessRunner(ProcessRunner ffmpeg) {
        this.processRunner = ffmpeg;
    }
}
