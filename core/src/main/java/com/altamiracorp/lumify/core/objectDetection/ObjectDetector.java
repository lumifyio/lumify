package com.altamiracorp.lumify.core.objectDetection;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrame;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrameRepository;
import com.altamiracorp.lumify.core.user.User;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public abstract class ObjectDetector {
    private final ArtifactRepository artifactRepository;
    private final VideoFrameRepository videoFrameRepository;

    public ObjectDetector(ArtifactRepository artifactRepository, VideoFrameRepository videoFrameRepository) {
        this.artifactRepository = artifactRepository;
        this.videoFrameRepository = videoFrameRepository;
    }

    public List<ArtifactDetectedObject> detectObjects(VideoFrame videoFrame, User user) throws IOException {
        BufferedImage bImage = videoFrameRepository.loadImage(videoFrame, user);
        return detectObjects(bImage);
    }

    public abstract void setup(String classifierPath, InputStream dictionary) throws IOException;

    public abstract void setup(String classifierPath) throws IOException;

    public abstract List<ArtifactDetectedObject> detectObjects(BufferedImage bImage) throws IOException;

    public abstract String getModelName();

    public void init(Map map) {

    }
}
