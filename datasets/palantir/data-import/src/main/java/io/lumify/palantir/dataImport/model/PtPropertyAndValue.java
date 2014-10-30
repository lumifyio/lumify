package io.lumify.palantir.dataImport.model;

import java.sql.Struct;

public class PtPropertyAndValue {
    private long id;
    private long realmId;
    private long linkObjectId;
    private long dataEventId;
    private Long originDataEventId;
    private boolean deleted;
    private long propertyValueId;
    private long crossResolutionId;
    private long accessControlListId;
    private long lastModifiedBy;
    private long lastModified;
    private long type;
    private String value;
    private long linkRoleId;
    private long linkType;
    private long priority;
    private boolean userDisabledKeyword;
    private String customKeywordTerm;
    private String geometryXml;
    private Long timeStart;
    private Long timeEnd;
    private long propertyStatus;
    private long createdBy;
    private long timeCreated;
    private Struct geometryGis;

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

    public long getPropertyValueId() {
        return propertyValueId;
    }

    public void setPropertyValueId(long propertyValueId) {
        this.propertyValueId = propertyValueId;
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

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getLinkRoleId() {
        return linkRoleId;
    }

    public void setLinkRoleId(long linkRoleId) {
        this.linkRoleId = linkRoleId;
    }

    public long getLinkType() {
        return linkType;
    }

    public void setLinkType(long linkType) {
        this.linkType = linkType;
    }

    public long getPriority() {
        return priority;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }

    public boolean isUserDisabledKeyword() {
        return userDisabledKeyword;
    }

    public void setUserDisabledKeyword(boolean userDisabledKeyword) {
        this.userDisabledKeyword = userDisabledKeyword;
    }

    public String getCustomKeywordTerm() {
        return customKeywordTerm;
    }

    public void setCustomKeywordTerm(String customKeywordTerm) {
        this.customKeywordTerm = customKeywordTerm;
    }

    public String getGeometryXml() {
        return geometryXml;
    }

    public void setGeometryXml(String geometryXml) {
        this.geometryXml = geometryXml;
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

    public long getPropertyStatus() {
        return propertyStatus;
    }

    public void setPropertyStatus(long propertyStatus) {
        this.propertyStatus = propertyStatus;
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

    public Struct getGeometryGis() {
        return geometryGis;
    }

    public void setGeometryGis(Struct geometryGis) {
        this.geometryGis = geometryGis;
    }
}
