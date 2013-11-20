package com.altamiracorp.lumify.storm.file;

import java.io.File;

public class FileMetadata {
    private String fileName;
    private String mimeType;
    private File primaryFileFromArchive;

    public FileMetadata(String fileName, String mimeType) {
        this.fileName = fileName;
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public File getPrimaryFileFromArchive() {
        return primaryFileFromArchive;
    }

    public void setPrimaryFileFromArchive(File primaryFileFromArchive) {
        this.primaryFileFromArchive = primaryFileFromArchive;
    }
}
