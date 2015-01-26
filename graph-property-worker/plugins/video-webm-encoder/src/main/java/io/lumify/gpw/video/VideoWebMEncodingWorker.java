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

public class VideoWebMEncodingWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VideoWebMEncodingWorker.class);
    private static final String PROPERTY_KEY = VideoWebMEncodingWorker.class.getName();
    private ProcessRunner processRunner;
    private IntegerLumifyProperty videoRotationProperty;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        videoRotationProperty = new IntegerLumifyProperty(getOntologyRepository().getRequiredPropertyIRIByIntent("media.clockwiseRotation"));
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File webmFile = File.createTempFile("encode_webm_", ".webm");
        String[] ffmpegOptionsArray = prepareFFMPEGOptions(data, webmFile);
        try {
            processRunner.execute(
                    "ffmpeg",
                    ffmpegOptionsArray,
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

            try (InputStream webmFileIn = new FileInputStream(webmFile)) {
                StreamingPropertyValue spv = new StreamingPropertyValue(webmFileIn, byte[].class);
                spv.searchIndex(false);
                Metadata metadata = new Metadata();
                metadata.add(LumifyProperties.MIME_TYPE.getPropertyName(), MediaLumifyProperties.MIME_TYPE_VIDEO_WEBM, getVisibilityTranslator().getDefaultVisibility());
                MediaLumifyProperties.VIDEO_WEBM.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
            }
        } finally {
            if (!webmFile.delete()) {
                LOGGER.warn("Could not delete %s", webmFile.getAbsolutePath());
            }
        }
    }

    private String[] prepareFFMPEGOptions(GraphPropertyWorkData data, File webmFile) {
        ArrayList<String> ffmpegOptionsList = new ArrayList<String>();

        ffmpegOptionsList.add("-y");
        ffmpegOptionsList.add("-i");
        ffmpegOptionsList.add(data.getLocalFile().getAbsolutePath());
        ffmpegOptionsList.add("-vcodec");
        ffmpegOptionsList.add("libvpx");
        ffmpegOptionsList.add("-b:v");
        ffmpegOptionsList.add("600k");
        ffmpegOptionsList.add("-qmin");
        ffmpegOptionsList.add("10");
        ffmpegOptionsList.add("-qmax");
        ffmpegOptionsList.add("42");
        ffmpegOptionsList.add("-maxrate");
        ffmpegOptionsList.add("500k");
        ffmpegOptionsList.add("-bufsize");
        ffmpegOptionsList.add("1000k");
        ffmpegOptionsList.add("-threads");
        ffmpegOptionsList.add("2");

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

        ffmpegOptionsList.add("-acodec");
        ffmpegOptionsList.add("libvorbis");
        ffmpegOptionsList.add("-map");
        ffmpegOptionsList.add("0");
        ffmpegOptionsList.add("-map");
        ffmpegOptionsList.add("-0:s");
        ffmpegOptionsList.add("-f");
        ffmpegOptionsList.add("webm");
        ffmpegOptionsList.add(webmFile.getAbsolutePath());
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

        if (MediaLumifyProperties.VIDEO_WEBM.hasProperty(element, PROPERTY_KEY)) {
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
