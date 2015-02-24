package io.lumify.palantir.model;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PtTypeGroup extends PtModelBase {
    private long type;
    private String config;
    private boolean hidden;
    private long createdBy;
    private long timeCreated;
    private long lastModified;

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(long createdBy) {
        this.createdBy = createdBy;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public Writable getKey() {
        return new LongWritable(getType());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(getType());
        writeFieldNullableString(out, getConfig());
        out.writeBoolean(isHidden());
        out.writeLong(getCreatedBy());
        out.writeLong(getTimeCreated());
        out.writeLong(getLastModified());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        setType(in.readLong());
        setConfig(readFieldNullableString(in));
        setHidden(in.readBoolean());
        setCreatedBy(in.readLong());
        setTimeCreated(in.readLong());
        setLastModified(in.readLong());
    }
}
