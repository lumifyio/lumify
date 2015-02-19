package io.lumify.palantir.model;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PtObjectObject extends PtModelBase {
    private long linkId;
    private long realmId;
    private long parentObjectId;
    private long childObjectId;
    private long childRoleId;
    private long type;
    private String text;
    private long dataEventId;
    private Long originDataEventId;
    private long crossResolutionId;
    private boolean deleted;
    private Long timeStart;
    private Long timeEnd;
    private long accessControlListId;
    private long createdBy;
    private long timeCreated;
    private long lastModified;
    private long lastModifiedBy;

    public long getLinkId() {
        return linkId;
    }

    public void setLinkId(long linkId) {
        this.linkId = linkId;
    }

    public long getRealmId() {
        return realmId;
    }

    public void setRealmId(long realmId) {
        this.realmId = realmId;
    }

    public long getParentObjectId() {
        return parentObjectId;
    }

    public void setParentObjectId(long parentObjectId) {
        this.parentObjectId = parentObjectId;
    }

    public long getChildObjectId() {
        return childObjectId;
    }

    public void setChildObjectId(long childObjectId) {
        this.childObjectId = childObjectId;
    }

    public long getChildRoleId() {
        return childRoleId;
    }

    public void setChildRoleId(long childRoleId) {
        this.childRoleId = childRoleId;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public long getCrossResolutionId() {
        return crossResolutionId;
    }

    public void setCrossResolutionId(long crossResolutionId) {
        this.crossResolutionId = crossResolutionId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Long timeStart) {
        this.timeStart = timeStart;
    }

    public Long getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Long timeEnd) {
        this.timeEnd = timeEnd;
    }

    public long getAccessControlListId() {
        return accessControlListId;
    }

    public void setAccessControlListId(long accessControlListId) {
        this.accessControlListId = accessControlListId;
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
        return new LongWritable(getLinkId());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(getLinkId());
        out.writeLong(getRealmId());
        out.writeLong(getParentObjectId());
        out.writeLong(getChildObjectId());
        out.writeLong(getChildRoleId());
        out.writeLong(getType());
        writeFieldNullableString(out, getText());
        out.writeLong(getDataEventId());
        writeFieldNullableLong(out, getOriginDataEventId());
        out.writeLong(getCrossResolutionId());
        out.writeBoolean(isDeleted());
        writeFieldNullableLong(out, getTimeStart());
        writeFieldNullableLong(out, getTimeEnd());
        out.writeLong(getAccessControlListId());
        out.writeLong(getCreatedBy());
        out.writeLong(getTimeCreated());
        out.writeLong(getLastModified());
        out.writeLong(getLastModifiedBy());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        setLinkId(in.readLong());
        setRealmId(in.readLong());
        setParentObjectId(in.readLong());
        setChildObjectId(in.readLong());
        setChildRoleId(in.readLong());
        setType(in.readLong());
        setText(readFieldNullableString(in));
        setDataEventId(in.readLong());
        setOriginDataEventId(readFieldNullableLong(in));
        setCrossResolutionId(in.readLong());
        setDeleted(in.readBoolean());
        setTimeStart(readFieldNullableLong(in));
        setTimeEnd(readFieldNullableLong(in));
        setAccessControlListId(in.readLong());
        setCreatedBy(in.readLong());
        setTimeCreated(in.readLong());
        setLastModified(in.readLong());
        setLastModifiedBy(in.readLong());
    }
}
