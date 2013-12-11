package com.altamiracorp.lumify.core.model.videoFrames;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

public class VideoFrameMetadata extends ColumnFamily {
    public static final String NAME = "metadata";
    private static final String DATA = "data";
    private static final String TEXT = "text";
    private static final String START_TIME = "start_time";
    private static final String END_TIME = "end_time";

    public VideoFrameMetadata() {
        super(NAME);
    }

    public byte[] getData() {
        return Value.toBytes(get(DATA));
    }

    public VideoFrameMetadata setData(byte[] data) {
        set(DATA, data);
        return this;
    }

    public String getText() {
        return Value.toString(get(TEXT));
    }

    public VideoFrameMetadata setText(String text) {
        set(TEXT, text);
        return this;
    }
}
