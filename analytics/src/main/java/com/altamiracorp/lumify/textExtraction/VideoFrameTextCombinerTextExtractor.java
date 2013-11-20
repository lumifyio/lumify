package com.altamiracorp.lumify.textExtraction;

import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrame;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrameRepository;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.hadoop.mapreduce.Mapper;

public class VideoFrameTextCombinerTextExtractor implements TextExtractor {
    private static final String NAME = "videoFrameTextCombinerExtractor";

    private VideoFrameRepository videoFrameRepository;

    @Inject
    public VideoFrameTextCombinerTextExtractor(VideoFrameRepository videoFrameRepository) {
        this.videoFrameRepository = videoFrameRepository;
    }

    @Override
    public void setup(Mapper.Context context, Injector injector) {
    }

    @Override
    public ArtifactExtractedInfo extract(Artifact artifact, User user) throws Exception {
        throw new RuntimeException("storm refactor - not implemented"); // TODO storm refactor
//        if (artifact.getType() != ArtifactType.VIDEO) {
//            return null;
//        }
//
//        VideoTranscript transcript = artifact.getContent().getVideoTranscript();
//        if (transcript == null) {
//            transcript = new VideoTranscript();
//        }
//        List<VideoFrame> videoFrames = videoFrameRepository.findAllByArtifactRowKey(artifact.getRowKey().toString(), user);
//        for (VideoFrame videoFrame : videoFrames) {
//            VideoTranscript.Time time = new VideoTranscript.Time(videoFrame.getRowKey().getTime(), null);
//            String text = videoFrame.getMetadata().getText();
//            if (text != null) {
//                transcript.add(time, text);
//            }
//        }
//
//        artifact.getContent().mergeVideoTranscript(transcript);
//
//        ArtifactExtractedInfo extractedInfo = new ArtifactExtractedInfo();
//        extractedInfo.setText(transcript.toString());
//        return extractedInfo;
    }

    @Override
    public VideoFrameExtractedInfo extract(VideoFrame videoFrame, User user) throws Exception {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }


}
