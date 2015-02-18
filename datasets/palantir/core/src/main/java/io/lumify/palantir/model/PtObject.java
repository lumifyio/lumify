package io.lumify.palantir.model;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PtObject extends PtModelBase {
    private long objectId;
    private long realmId;
    private long type;
    private boolean isGroup;
    private boolean resolved;
    private long dataEventId;
    private Long originDataEventId;
    private Long deleted;
    private long createdBy;
    private long timeCreated;
    private long lastModified;
    private long lastModifiedBy;

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public long getRealmId() {
        return realmId;
    }

    public void setRealmId(long realmId) {
        this.realmId = realmId;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setIsGroup(boolean isGroup) {
        this.isGroup = isGroup;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public long getDataEventId() {
        return dataEventId;
    }

    public void setDataEventId(long dataEventId) {
        this.dataEventId = dataEventId;
    }

    public Long getOriginDataEventId() {
        return originDataEventId;
    }

    public void setOriginDataEventId(Long originDataEventId) {
        this.originDataEventId = originDataEventId;
    }

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(Long deleted) {
        if (deleted == null || deleted == 0) {
            deleted = null;
        }
        this.deleted = deleted;
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

    public long getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(long lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public Writable getKey() {
        return new LongWritable(getObjectId());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(getObjectId());
        out.writeLong(getRealmId());
        out.writeLong(getType());
        out.writeBoolean(isGroup());
        out.writeBoolean(isResolved());
        out.writeLong(getDataEventId());
        writeFieldNullableLong(out, getOriginDataEventId());
        writeFieldNullableLong(out, getDeleted());
        out.writeLong(getCreatedBy());
        out.writeLong(getTimeCreated());
        out.writeLong(getLastModified());
        out.writeLong(getLastModifiedBy());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        setObjectId(in.readLong());
        setRealmId(in.readLong());
        setType(in.readLong());
        setIsGroup(in.readBoolean());
        setResolved(in.readBoolean());
        setDataEventId(in.readLong());
        setOriginDataEventId(readFieldNullableLong(in));
        setDeleted(readFieldNullableLong(in));
        setCreatedBy(in.readLong());
        setTimeCreated(in.readLong());
        setLastModified(in.readLong());
        setLastModifiedBy(in.readLong());
    }
}
