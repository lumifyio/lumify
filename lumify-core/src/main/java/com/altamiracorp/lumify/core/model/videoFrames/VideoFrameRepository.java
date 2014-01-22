package com.altamiracorp.lumify.core.model.videoFrames;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.user.User;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class VideoFrameRepository extends Repository<VideoFrame> {
    private VideoFrameBuilder videoFrameBuilder = new VideoFrameBuilder();

    @Inject
    public VideoFrameRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public VideoFrame fromRow(Row row) {
        return videoFrameBuilder.fromRow(row);
    }

    @Override
    public Row toRow(VideoFrame videoFrame) {
        return videoFrame;
    }

    @Override
    public String getTableName() {
        return videoFrameBuilder.getTableName();
    }

    public void saveVideoFrame(Object artifactVertexId, InputStream in, long frameStartTime, User user) throws IOException {
        byte[] data = IOUtils.toByteArray(in);
        VideoFrameRowKey videoFrameRowKey = new VideoFrameRowKey(artifactVertexId.toString(), frameStartTime);
        VideoFrame videoFrame = new VideoFrame(videoFrameRowKey);
        videoFrame.getMetadata().setData(data);
        save(videoFrame, user.getModelUserContext());
    }

    public Iterable<VideoFrame> findAllByArtifactRowKey(String rowKey, User user) {
        return findByRowStartsWith(rowKey, user.getModelUserContext());
    }

    public BufferedImage loadImage(VideoFrame videoFrame, User user) {
        InputStream in = new ByteArrayInputStream(videoFrame.getMetadata().getData());
        try {
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not load image: " + videoFrame.getRowKey(), e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not close InputStream", e);
            }
        }
    }
}