package io.lumify.palantir.dataImport.model;

public class PtLinkRelation {
    private long id;
    private long tableTypeId1;
    private String uri1;
    private long tableTypeId2;
    private String uri2;
    private String linkUri;
    private long linkStatus;
    private boolean hidden;
    private long createdBy;
    private long timeCreated;
    private long lastModified;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTableTypeId1() {
        return tableTypeId1;
    }

    public void setTableTypeId1(long tableTypeId1) {
        this.tableTypeId1 = tableTypeId1;
    }

    public String getUri1() {
        return uri1;
    }

    public void setUri1(String uri1) {
        this.uri1 = uri1;
    }

    public long getTableTypeId2() {
        return tableTypeId2;
    }

    public void setTableTypeId2(long tableTypeId2) {
        this.tableTypeId2 = tableTypeId2;
    }

    public String getUri2() {
        return uri2;
    }

    public void setUri2(String uri2) {
        this.uri2 = uri2;
    }

    public String getLinkUri() {
        return linkUri;
    }

    public void setLinkUri(String linkUri) {
        this.linkUri = linkUri;
    }

    public long getLinkStatus() {
        return linkStatus;
    }

    public void setLinkStatus(long linkStatus) {
        this.linkStatus = linkStatus;
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
}
