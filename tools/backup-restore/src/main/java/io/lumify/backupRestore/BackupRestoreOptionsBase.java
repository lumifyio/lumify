package io.lumify.backupRestore;

public abstract class BackupRestoreOptionsBase {
    private String accumuloPassword;
    private String accumuloUserName;
    private String accumuloInstanceName;
    private String zookeeperServers;
    private String hadoopFsDefaultFS;
    private boolean hadoopDfsClientUseDatanodeHostname;
    private String hadoopUsername;

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

    public String getHadoopFsDefaultFS() {
        if (!hadoopFsDefaultFS.startsWith("hdfs://")) {
            return "hdfs://" + hadoopFsDefaultFS;
        }
        return hadoopFsDefaultFS;
    }

    public BackupRestoreOptionsBase setHadoopFsDefaultFS(String hadoopFsDefaultFS) {
        this.hadoopFsDefaultFS = hadoopFsDefaultFS;
        return this;
    }

    protected String getWithHdfsLocation(String dir) {
        if (!dir.startsWith("hdfs:")) {
            if (!dir.startsWith("/")) {
                dir = "/" + dir;
            }
            return getHadoopFsDefaultFS() + dir;
        }
        return dir;
    }

    public boolean isHadoopDfsClientUseDatanodeHostname() {
        return hadoopDfsClientUseDatanodeHostname;
    }

    public BackupRestoreOptionsBase setHadoopDfsClientUseDatanodeHostname(boolean hadoopDfsClientUseDatanodeHostname) {
        this.hadoopDfsClientUseDatanodeHostname = hadoopDfsClientUseDatanodeHostname;
        return this;
    }

    public String getHadoopUsername() {
        return hadoopUsername;
    }

    public BackupRestoreOptionsBase setHadoopUsername(String hadoopUsername) {
        this.hadoopUsername = hadoopUsername;
        return this;
    }
}
