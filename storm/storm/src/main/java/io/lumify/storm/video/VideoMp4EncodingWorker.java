package io.lumify.storm.video;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import io.lumify.storm.util.JSONExtractor;
import io.lumify.storm.util.StringUtil;
import io.lumify.storm.util.VideoDimensionsUtil;
import io.lumify.storm.util.VideoRotationUtil;
import org.json.JSONObject;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VideoMp4EncodingWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = VideoMp4EncodingWorker.class.getName();
    private ProcessRunner processRunner;
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VideoMp4EncodingWorker.class);


    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp4File = File.createTempFile("encode_mp4_", ".mp4");
        File mp4RelocatedFile = File.createTempFile("relocated_mp4_", ".mp4");
        String[] ffmpegOptionsArray = prepareFFMPEGOptionsForMp4(data, mp4File);
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

            InputStream mp4RelocatedFileIn = new FileInputStream(mp4RelocatedFile);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4RelocatedFileIn, byte[].class);
                spv.searchIndex(false);
                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put(RawLumifyProperties.MIME_TYPE.getPropertyName(), MediaLumifyProperties.MIME_TYPE_VIDEO_MP4);
                MediaLumifyProperties.VIDEO_MP4.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
            } finally {
                mp4RelocatedFileIn.close();
            }
        } finally {
            mp4File.delete();
            mp4RelocatedFile.delete();
        }
    }

    public String[] prepareFFMPEGOptionsForMp4(GraphPropertyWorkData data, File mp4File) {
        JSONObject json = JSONExtractor.retrieveJSONObjectUsingFFPROBE(processRunner, data);
        Integer videoRotation = VideoRotationUtil.extractRotationFromJSON(json);
        if (videoRotation == null)
            videoRotation = 0;
        String[] ffmpegRotationOptions = VideoRotationUtil.createFFMPEGRotationOptions(videoRotation);

        Integer videoWidth = VideoDimensionsUtil.extractWidthFromJSON(json);
        Integer videoHeight = VideoDimensionsUtil.extractHeightFromJSON(json);
        int[] displayDimensions = VideoDimensionsUtil.calculateDisplayDimensions(videoWidth, videoHeight, videoRotation);

        LOGGER.debug("videoWidth = " + videoWidth);
        LOGGER.debug("videoHeight = " + videoHeight);
        LOGGER.debug("ffmpegScaleOptions = " + displayDimensions);

        ArrayList<String> ffmpegOptionsList = new ArrayList<String>();
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
        ffmpegOptionsList.add("-vf");
        ffmpegOptionsList.add("scale=" + displayDimensions[0] + ":" + displayDimensions[1]);
        if (ffmpegRotationOptions != null) {
            ffmpegOptionsList.add(ffmpegRotationOptions[0]);
            ffmpegOptionsList.add(ffmpegRotationOptions[1]);
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
        String[] ffmpegOptionsArray = StringUtil.createStringArrayFromList(ffmpegOptionsList);
        return ffmpegOptionsArray;
        //TODO. Should scale always be 720:480?
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(RawLumifyProperties.RAW.getPropertyName())) {
            return false;
        }
        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.MIME_TYPE.getPropertyName());
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
