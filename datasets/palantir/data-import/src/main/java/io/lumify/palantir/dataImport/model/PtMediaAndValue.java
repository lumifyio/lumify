package io.lumify.palantir.dataImport.model;

import java.io.InputStream;

public class PtMediaAndValue {
    private long id;
    private long realmId;
    private long linkObjectId;
    private long dataEventId;
    private Long originDataEventId;
    private boolean deleted;
    private long mediaValueId;
    private long crossResolutionId;
    private long accessControlListId;
    private long createdBy;
    private long timeCreated;
    private long lastModifiedBy;
    private long lastModified;
    private String title;
    private String description;
    private long linkType;
    private long type;
    private InputStream contents;
    private byte[] contentsHash;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRealmId() {
        return realmId;
    }

    public void setRealmId(long realmId) {
        this.realmId = realmId;
    }

    public long getLinkObjectId() {
        return linkObjectId;
    }

    public void setLinkObjectId(long linkObjectId) {
        this.linkObjectId = linkObjectId;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public long getMediaValueId() {
        return mediaValueId;
    }

    public void setMediaValueId(long mediaValueId) {
        this.mediaValueId = mediaValueId;
    }

    public long getCrossResolutionId() {
        return crossResolutionId;
    }

    public void setCrossResolutionId(long crossResolutionId) {
        this.crossResolutionId = crossResolutionId;
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

    public long getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(long lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
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

    public long getLinkType() {
        return linkType;
    }

    public void setLinkType(long linkType) {
        this.linkType = linkType;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public InputStream getContents() {
        return contents;
    }

    public void setContents(InputStream contents) {
        this.contents = contents;
    }

    public byte[] getContentsHash() {
        return contentsHash;
    }

    public void setContentsHash(byte[] contentsHash) {
        this.contentsHash = contentsHash;
    }
}
