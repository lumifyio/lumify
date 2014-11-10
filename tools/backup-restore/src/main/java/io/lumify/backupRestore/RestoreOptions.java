package io.lumify.backupRestore;

public class RestoreOptions extends BackupRestoreOptionsBase {
    private String hdfsRestoreDirectory;

    public String getHdfsRestoreDirectory() {
        return getWithHdfsLocation(hdfsRestoreDirectory);
    }

    public RestoreOptions setHdfsRestoreDirectory(String hdfsRestoreDirectory) {
        this.hdfsRestoreDirectory = hdfsRestoreDirectory;
        return this;
    }
}
