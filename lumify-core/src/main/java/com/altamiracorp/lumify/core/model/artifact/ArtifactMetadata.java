package com.altamiracorp.lumify.core.model.artifact;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.lumify.core.ingest.video.VideoTranscript;

import java.util.Date;

public class ArtifactMetadata extends ColumnFamily {
    public static final String NAME = "Generic_Metadata";
    public static final String RAW = "raw";
    public static final String TEXT = "text";
    public static final String HIGHLIGHTED_TEXT = "highlightedText";
    private static final String GRAPH_VERTEX_ID = "graphVertexId";
    private static final String CREATE_DATE = "createDate";
    private static final String PUBLISH_DATE = "publishDate";
    private static final String VIDEO_TRANSCRIPT = "videoTranscript";
    private static final String MAPPING_JSON = "mappingJson";
    private static final String MIME_TYPE = "mimeType";
    private static final String FILE_EXTENSION = "fileExtension";
    private static final String FILE_NAME = "fileName";
    private static final String VIDEO_DURATION = "videoDuration";

    public ArtifactMetadata() {
        super(NAME);
    }

    public byte[] getRaw() {
        return Value.toBytes(get(RAW));
    }

    public ArtifactMetadata setRaw(byte[] raw) {
        set(RAW, raw);
        return this;
    }

    public String getText() {
        return Value.toString(get(TEXT));
    }

    public ArtifactMetadata setText(String text) {
        set(TEXT, text);
        return this;
    }

    public String getPublishDate() {
        return Value.toString(get(PUBLISH_DATE));
    }

    public ArtifactMetadata setPublishDate(String publishDate) {
        set(PUBLISH_DATE, publishDate);
        return this;
    }

    public String getHighlightedText() {
        return Value.toString(get(HIGHLIGHTED_TEXT));
    }

    public ArtifactMetadata setHighlightedText(String highlightedText) {
        set(HIGHLIGHTED_TEXT, highlightedText);
        return this;
    }

    public Object getGraphVertexId() {
        return Value.toString(get(GRAPH_VERTEX_ID));
    }

    public ArtifactMetadata setGraphVertexId(Object graphVertexId) {
        set(GRAPH_VERTEX_ID, graphVertexId);
        return this;
    }

    public ArtifactMetadata setCreateDate(Date createDate) {
        set(CREATE_DATE, createDate.getTime());
        return this;
    }

    public ArtifactMetadata setVideoTranscript(VideoTranscript videoTranscript) {
        set(VIDEO_TRANSCRIPT, videoTranscript.toJson().toString());
        return this;
    }

    public ArtifactMetadata setMappingJson(String mappingJson) {
        set(MAPPING_JSON, mappingJson);
        return this;
    }

    public String getMappingJson() {
        if (get(MAPPING_JSON) != null) {
            return Value.toString(get(MAPPING_JSON));
        }
        return null;
    }

    public String getMimeType() {
        return Value.toString(get(MIME_TYPE));
    }

    public ArtifactMetadata setMimeType(String mimeType) {
        set(MIME_TYPE, mimeType);
        return this;
    }

    public String getFileExtension() {
        return Value.toString(get(FILE_EXTENSION));
    }

    public ArtifactMetadata setFileExtension(String fileExtension) {
        set(FILE_EXTENSION, fileExtension);
        return this;
    }

    public String getFileName() {
        return Value.toString(get(FILE_NAME));
    }

    public ArtifactMetadata setVideoDuration(String videoDuration) {
        set(VIDEO_DURATION, videoDuration);
        return this;
    }

    public ArtifactMetadata setFileName(String fileName) {
        set(FILE_NAME, fileName);
        return this;
    }
}
