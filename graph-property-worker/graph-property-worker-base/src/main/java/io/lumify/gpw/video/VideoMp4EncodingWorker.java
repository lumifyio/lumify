package io.lumify.gpw.video;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.properties.types.IntegerLumifyProperty;
import io.lumify.core.util.ProcessRunner;
import io.lumify.gpw.util.FFprobeRotationUtil;
import io.lumify.gpw.MediaPropertyConfiguration;
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
    private MediaPropertyConfiguration config = new MediaPropertyConfiguration();
    private ProcessRunner processRunner;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        getConfiguration().setConfigurables(config, MediaPropertyConfiguration.PROPERTY_NAME_PREFIX);
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

            InputStream mp4RelocatedFileIn = new FileInputStream(mp4RelocatedFile);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4RelocatedFileIn, byte[].class);
                spv.searchIndex(false);
                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put(LumifyProperties.MIME_TYPE.getPropertyName(), MediaLumifyProperties.MIME_TYPE_VIDEO_MP4);
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

    public String[] prepareFFMPEGOptions(GraphPropertyWorkData data, File mp4File) {
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

        //Scale.
        //Will not force conversion to 720:480 aspect ratio, but will resize video with original aspect ratio.
        ffmpegOptionsList.add("-vf");
        ffmpegOptionsList.add("scale=720:480");

        IntegerLumifyProperty videoRotationProperty = new IntegerLumifyProperty(config.clockwiseRotationIri);
        Integer videoRotation = videoRotationProperty.getPropertyValue(data.getElement(), 0);
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
        String[] ffmpegOptionsArray = ffmpegOptionsList.toArray(new String[ffmpegOptionsList.size()]);
        return ffmpegOptionsArray;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }
        String mimeType = (String) property.getMetadata().get(LumifyProperties.MIME_TYPE.getPropertyName());
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
