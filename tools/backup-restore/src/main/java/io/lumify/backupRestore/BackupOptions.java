package io.lumify.backupRestore;

public class BackupOptions extends BackupRestoreOptionsBase {
    private String tableNamePrefix;
    private String hdfsBackupDirectory;

    public BackupOptions setTableNamePrefix(String tableNamePrefix) {
        this.tableNamePrefix = tableNamePrefix;
        return this;
    }

    public String getTableNamePrefix() {
        return tableNamePrefix;
    }

    public BackupOptions setHdfsBackupDirectory(String hdfsBackupDirectory) {
        this.hdfsBackupDirectory = hdfsBackupDirectory;
        return this;
    }

    public String getHdfsBackupDirectory() {
        return hdfsBackupDirectory;
    }
}
