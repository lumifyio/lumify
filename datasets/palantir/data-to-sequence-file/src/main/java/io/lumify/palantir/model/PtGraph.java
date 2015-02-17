package io.lumify.palantir.model;

public class PtGraph {
    private long id;
    private long dataEventId;
    private long sourceRealmId;
    private boolean pending;
    private long createdBy;
    private long timeCreated;
    private String title;
    private String description;
    private byte[] thumbnail;
    private byte[] awstateProto;
    private byte[] extProps;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDataEventId() {
        return dataEventId;
    }

    public void setDataEventId(long dataEventId) {
        this.dataEventId = dataEventId;
    }

    public long getSourceRealmId() {
        return sourceRealmId;
    }

    public void setSourceRealmId(long sourceRealmId) {
        this.sourceRealmId = sourceRealmId;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    public byte[] getAwstateProto() {
        return awstateProto;
    }

    public void setAwstateProto(byte[] awstateProto) {
        this.awstateProto = awstateProto;
    }

    public byte[] getExtProps() {
        return extProps;
    }

    public void setExtProps(byte[] extProps) {
        this.extProps = extProps;
    }
}
