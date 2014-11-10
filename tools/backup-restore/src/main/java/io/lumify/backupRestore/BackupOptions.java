package io.lumify.backupRestore;

public class BackupOptions extends BackupRestoreOptionsBase {
    private String tableNamePrefix;

    public BackupOptions setTableNamePrefix(String tableNamePrefix) {
        this.tableNamePrefix = tableNamePrefix;
        return this;
    }

    public String getTableNamePrefix() {
        return tableNamePrefix;
    }
}
