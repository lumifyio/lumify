package io.lumify.analystsNotebook;

import io.lumify.core.config.Configurable;
import io.lumify.core.config.PostConfigurationValidator;

public class AnalystsNotebookExportConfiguration {
    public static final String CONFIGURATION_PREFIX = "analystsNotebookExport";

    private boolean enableCustomImageCollection;
    private boolean enableIconPicture;
    private int thumbnailWidth;
    private int thumbnailHeight;
    private boolean includeProperties;
    private boolean includeSubtitle;
    private boolean includeTime;
    private boolean includeImageUrl;
    private boolean includeVisibility;
    private String visibilityLabel;

    @Configurable(name = "enableCustomImageCollection", defaultValue = "true")
    public void setEnableCustomImageCollection(String enableCustomImageCollection) {
        this.enableCustomImageCollection = Boolean.valueOf(enableCustomImageCollection);
    }

    @Configurable(name = "enableIconPicture", defaultValue = "true")
    public void setEnableIconPicture(String enableIconPicture) {
        this.enableIconPicture = Boolean.valueOf(enableIconPicture);
    }

    @Configurable(name = "thumbnailWidth", defaultValue = "200")
    public void setThumbnailWidth(int thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    @Configurable(name = "thumbnailHeight", defaultValue = "200")
    public void setThumbnailHeight(int thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }

    @Configurable(name = "includeProperties", defaultValue = "true")
    public void setIncludeProperties(String includeProperties) {
        this.includeProperties = Boolean.valueOf(includeProperties);
    }

    @Configurable(name = "includeSubtitle", defaultValue = "true")
    public void setIncludeSubtitle(String includeSubtitle) {
        this.includeSubtitle = Boolean.valueOf(includeSubtitle);
    }

    @Configurable(name = "includeTime", defaultValue = "true")
    public void setIncludeTime(String includeTime) {
        this.includeTime = Boolean.valueOf(includeTime);
    }

    @Configurable(name = "includeImageUrl", defaultValue = "true")
    public void setIncludeImageUrl(String includeImageUrl) {
        this.includeImageUrl = Boolean.valueOf(includeImageUrl);
    }

    @Configurable(name = "includeVisibility", defaultValue = "false")
    public void setIncludeVisibility(String includeVisibility) {
        this.includeVisibility = Boolean.valueOf(includeVisibility);
    }

    @Configurable(name = "visibilityLabel", required = false)
    public void setVisibilityLabel(String visibilityLabel) {
        this.visibilityLabel = visibilityLabel;
    }

    @PostConfigurationValidator(description = "visibilityLabel must be configured if includeVisibility is true")
    public boolean validateVertexVisibility() {
        return !includeVisibility || visibilityLabel != null;
    }

    public boolean enableCustomImageCollection() {
        return enableCustomImageCollection;
    }

    public boolean enableIconPicture() {
        return enableIconPicture;
    }

    public int getThumbnailWidth() {
        return thumbnailWidth;
    }

    public int getThumbnailHeight() {
        return thumbnailHeight;
    }

    public boolean includeProperties() {
        return includeProperties;
    }

    public boolean includeSubtitle() {
        return includeSubtitle;
    }

    public boolean includeTime() {
        return includeTime;
    }

    public boolean includeImageUrl() {
        return includeImageUrl;
    }

    public boolean includeVisibility() {
        return includeVisibility;
    }

    public String getVisibilityLabel() {
        return visibilityLabel;
    }
}
