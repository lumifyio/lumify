package com.altamiracorp.lumify.core.model.videoFrames;

import com.altamiracorp.bigtable.model.BaseBuilder;
import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Row;

import java.util.Collection;

public class VideoFrameBuilder extends BaseBuilder<VideoFrame> {
    @Override
    public VideoFrame fromRow(Row row) {
        VideoFrame videoFrame = new VideoFrame(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            String columnFamilyName = columnFamily.getColumnFamilyName();
            if (columnFamilyName.equals(VideoFrameMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                videoFrame.addColumnFamily(new VideoFrameMetadata().addColumns(columns));
            } else if (columnFamilyName.equals(VideoFrameDetectedObjects.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                videoFrame.addColumnFamily(new VideoFrameDetectedObjects().addColumns(columns));
            } else {
                videoFrame.addColumnFamily(columnFamily);
            }
        }
        return videoFrame;
    }

    @Override
    public String getTableName() {
        return VideoFrame.TABLE_NAME;
    }
}
