package com.altamiracorp.lumify.core.ingest;

import org.apache.hadoop.fs.FileSystem;

import java.io.File;

public class AdditionalArtifactWorkData {

    private String mimeType;
    private String fileName;
    private FileSystem hdfsFileSystem;
    private String localFileName;
    private File archiveTempDir;

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public FileSystem getHdfsFileSystem() {
        return hdfsFileSystem;
    }

    public void setHdfsFileSystem(FileSystem hdfsFileSystem) {
        this.hdfsFileSystem = hdfsFileSystem;
    }

    public void setLocalFileName(String localFileName) {
        this.localFileName = localFileName;
    }

    public String getLocalFileName() {
        return localFileName;
    }

    public void setArchiveTempDir(File archiveTempDir) {
        this.archiveTempDir = archiveTempDir;
    }

    public File getArchiveTempDir() {
        return archiveTempDir;
    }
}
