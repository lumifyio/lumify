package io.lumify.backupRestore;

public abstract class BackupRestoreOptionsBase {
    private String accumuloPassword;
    private String accumuloUserName;
    private String accumuloInstanceName;
    private String zookeeperServers;
    private String hdfsBackupDirectory;
    private String hdfsLocation;

    public String getAccumuloPassword() {
        return accumuloPassword;
    }

    public BackupRestoreOptionsBase setAccumuloPassword(String accumuloPassword) {
        this.accumuloPassword = accumuloPassword;
        return this;
    }

    public String getAccumuloUserName() {
        return accumuloUserName;
    }

    public BackupRestoreOptionsBase setAccumuloUserName(String accumuloUserName) {
        this.accumuloUserName = accumuloUserName;
        return this;
    }

    public String getAccumuloInstanceName() {
        return accumuloInstanceName;
    }

    public BackupRestoreOptionsBase setAccumuloInstanceName(String accumuloInstanceName) {
        this.accumuloInstanceName = accumuloInstanceName;
        return this;
    }

    public String getZookeeperServers() {
        return zookeeperServers;
    }

    public BackupRestoreOptionsBase setZookeeperServers(String zookeeperServers) {
        this.zookeeperServers = zookeeperServers;
        return this;
    }

    public String getHdfsBackupDirectory() {
        return hdfsBackupDirectory;
    }

    public BackupRestoreOptionsBase setHdfsBackupDirectory(String hdfsBackupDirectory) {
        this.hdfsBackupDirectory = hdfsBackupDirectory;
        return this;
    }

    public String getHdfsLocation() {
        return hdfsLocation;
    }

    public BackupRestoreOptionsBase setHdfsLocation(String hdfsLocation) {
        this.hdfsLocation = hdfsLocation;
        return this;
    }
}
