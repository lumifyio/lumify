package com.altamiracorp.lumify.storm.file;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.InputStream;

public class FileMetadata {
    private String fileName;
    private String mimeType;
    private InputStream raw;
    private String title;
    private String source;
    private File primaryFileFromArchive;

    public FileMetadata() {
    }

    public String getFileName() {
        return fileName;
    }

    public FileMetadata setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileMetadata setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public File getPrimaryFileFromArchive() {
        return primaryFileFromArchive;
    }

    public void setPrimaryFileFromArchive(File primaryFileFromArchive) {
        this.primaryFileFromArchive = primaryFileFromArchive;
    }

    public InputStream getRaw() {
        return raw;
    }

    public FileMetadata setRaw(InputStream raw) {
        this.raw = raw;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FileMetadata setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSource() {
        return source;
    }

    public FileMetadata setSource(String source) {
        this.source = source;
        return this;
    }

    public String getFileNameWithoutDateSuffix() {
        String fname = FilenameUtils.getName(getFileName());
        int dateTimeSeparator = fname.lastIndexOf("__");
        if (dateTimeSeparator > 0) {
            fname = fname.substring(0, dateTimeSeparator);
        }
        return fname;
    }
}
