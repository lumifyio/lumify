package io.lumify.palantir.model;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PtNodeDisplayType extends PtOntologyType {
    private long id;
    private String name;
    private long createdBy;
    private long timeCreated;
    private long lastModified;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return new LongWritable(getId());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeLong(getId());
        out.writeUTF(getName());
        out.writeLong(getCreatedBy());
        out.writeLong(getTimeCreated());
        out.writeLong(getLastModified());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        throw new RuntimeException("Not Implemented");
    }
}
