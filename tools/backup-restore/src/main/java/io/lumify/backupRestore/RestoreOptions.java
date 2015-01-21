package io.lumify.backupRestore;

public class RestoreOptions extends BackupRestoreOptionsBase {
    private String hdfsRestoreDirectory;
    private String hdfsRestoreTempDirectory;

    public String getHdfsRestoreDirectory() {
        return getWithHdfsLocation(hdfsRestoreDirectory);
    }

    public RestoreOptions setHdfsRestoreDirectory(String hdfsRestoreDirectory) {
        this.hdfsRestoreDirectory = hdfsRestoreDirectory;
        return this;
    }

    public String getHdfsRestoreTempDirectory() {
        return hdfsRestoreTempDirectory;
    }

    public RestoreOptions setHdfsRestoreTempDirectory(String hdfsRestoreTempDirectory) {
        this.hdfsRestoreTempDirectory = hdfsRestoreTempDirectory;
        return this;
    }
}
