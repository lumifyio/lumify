package io.lumify.storm.video;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.properties.RawLumifyProperties;
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

public class VideoWebMEncodingWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = VideoWebMEncodingWorker.class.getName();
    private ProcessRunner processRunner;

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

            InputStream webmFileIn = new FileInputStream(webmFile);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(webmFileIn, byte[].class);
                spv.searchIndex(false);
                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put(RawLumifyProperties.MIME_TYPE.getPropertyName(), MediaLumifyProperties.MIME_TYPE_VIDEO_WEBM);
                MediaLumifyProperties.VIDEO_WEBM.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
            } finally {
                webmFileIn.close();
            }
        } finally {
            webmFile.delete();
        }
    }

    private String[] prepareFFMPEGOptions(GraphPropertyWorkData data, File webmFile) {
        JSONObject json = JSONExtractor.retrieveJSONObjectUsingFFPROBE(processRunner, data);
        Integer videoRotation = VideoRotationUtil.extractRotationFromJSON(json);
        if (videoRotation == null)
            videoRotation = 0;
        String[] ffmpegRotationOptions = VideoRotationUtil.createFFMPEGRotationOptions(videoRotation);

        Integer videoWidth = VideoDimensionsUtil.extractWidthFromJSON(json);
        Integer videoHeight = VideoDimensionsUtil.extractHeightFromJSON(json);
        int[] displayDimensions = VideoDimensionsUtil.calculateDisplayDimensions(videoWidth, videoHeight, videoRotation);

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
        ffmpegOptionsList.add("-vf");
        ffmpegOptionsList.add("scale=" + displayDimensions[0] + ":" + displayDimensions[1]);
        if (ffmpegRotationOptions != null) {
            ffmpegOptionsList.add(ffmpegRotationOptions[0]);
            ffmpegOptionsList.add(ffmpegRotationOptions[1]);
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
        String[] ffmpegOptionsArray = StringUtil.createStringArrayFromList(ffmpegOptionsList);
        return ffmpegOptionsArray;
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
