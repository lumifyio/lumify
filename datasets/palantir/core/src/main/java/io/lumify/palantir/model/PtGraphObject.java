package io.lumify.palantir.model;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PtGraphObject extends PtModelBase {
    private long graphId;
    private long objectId;

    public long getGraphId() {
        return graphId;
    }

    public void setGraphId(long graphId) {
        this.graphId = graphId;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    @Override
    public Writable getKey() {
        return new LongLongWritable(getGraphId(), getObjectId());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(getGraphId());
        out.writeLong(getObjectId());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        setGraphId(in.readLong());
        setObjectId(in.readLong());
    }
}
