package io.lumify.palantir.dataImport.model;

import io.lumify.core.exception.LumifyException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PtObjectType {
    private static final Pattern URI_PATTERN = Pattern.compile(".*<uri>(.*?)</uri>.*");
    private long type;
    private String config;
    private boolean hidden;
    private long createdBy;
    private long timeCreated;
    private long lastModified;
    private String uri;

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
        Matcher m = URI_PATTERN.matcher(this.config);
        if (m.matches()) {
            this.uri = m.group(1);
        } else {
            throw new LumifyException("Could not find uri in config: " + config);
        }
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

    public String getUri() {
        return this.uri;
    }
}
