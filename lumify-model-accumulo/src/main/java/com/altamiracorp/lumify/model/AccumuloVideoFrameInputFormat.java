package com.altamiracorp.lumify.model;

import com.altamiracorp.lumify.core.user.ModelAuthorizations;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrame;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrameBuilder;
import com.altamiracorp.bigtable.model.accumulo.AccumuloBaseInputFormat;
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.hadoop.mapreduce.Job;

public class AccumuloVideoFrameInputFormat extends AccumuloBaseInputFormat<VideoFrame, VideoFrameBuilder> {
    private VideoFrameBuilder videoFrameBuilder = new VideoFrameBuilder();

    public static void init(Job job, String username, String password, ModelAuthorizations modelAuthorizations, String zookeeperInstanceName, String zookeeperServerNames) {
        Authorizations authorizations = ((AccumuloModelAuthorizations) modelAuthorizations).getAuthorizations();
        AccumuloInputFormat.setZooKeeperInstance(job.getConfiguration(), zookeeperInstanceName, zookeeperServerNames);
        AccumuloInputFormat.setInputInfo(job.getConfiguration(), username, password.getBytes(), VideoFrame.TABLE_NAME, authorizations);
    }

    @Override
    public VideoFrameBuilder getBuilder() {
        return videoFrameBuilder;
    }
}
