package com.altamiracorp.lumify.textExtraction;

import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrame;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.google.inject.Injector;
import org.apache.hadoop.mapreduce.Mapper;

public interface TextExtractor {
    void setup(Mapper.Context context, Injector injector) throws Exception;

    ArtifactExtractedInfo extract(Artifact artifact, User user) throws Exception;

    VideoFrameExtractedInfo extract(VideoFrame videoFrame, User user) throws Exception;

    String getName();
}
