package io.lumify.core.model.artifactThumbnails;

public class ArtifactThumbnail {
    private byte[] thumbnail;
    private int type;
    private String format;

    public ArtifactThumbnail(byte[] thumbnail,
                             int type,
                             String format) {
        this.thumbnail = thumbnail;
        this.type = type;
        this.format = format;
    }

    public byte[] getThumbnailData() {
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
