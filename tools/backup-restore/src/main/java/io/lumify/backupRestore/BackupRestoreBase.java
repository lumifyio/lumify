package io.lumify.backupRestore;

import org.apache.accumulo.core.client.*;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.conf.Property;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public abstract class BackupRestoreBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestoreBase.class);

    protected Connector createAccumuloConnection(BackupRestoreOptionsBase options) throws AccumuloSecurityException, AccumuloException {
        String instanceName = options.getAccumuloInstanceName();
        String zooServers = options.getZookeeperServers();
        Instance inst = new ZooKeeperInstance(instanceName, zooServers);
        ConfigurationCopy conf = new ConfigurationCopy(inst.getConfiguration());
        conf.set(Property.INSTANCE_DFS_URI, options.getHadoopFsDefaultFS());
        inst.setConfiguration(conf);

        AuthenticationToken authenticationToken = new PasswordToken(options.getAccumuloPassword());
        return inst.getConnector(options.getAccumuloUserName(), authenticationToken);
    }

    protected FileSystem getHdfsFileSystem(BackupRestoreOptionsBase options) throws IOException, URISyntaxException, InterruptedException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", options.getHadoopFsDefaultFS());
        if (options.isHadoopDfsClientUseDatanodeHostname()) {
            conf.set("dfs.client.use.datanode.hostname", "true");
        }
        if (options.getHadoopUsername() != null) {
            return FileSystem.get(new URI(options.getHadoopFsDefaultFS()), conf, options.getHadoopUsername());
        }
        return FileSystem.get(conf);
    }

    protected void takeTableOnline(Connector conn, List<String> tableNames) throws AccumuloSecurityException, AccumuloException, TableNotFoundException {
        for (String tableName : tableNames) {
            LOGGER.debug("taking table " + tableName + " online");
            try {
                conn.tableOperations().online(tableName);
            } catch (Exception ex) {
                LOGGER.error("Failed to online table: " + tableName, ex);
            }
        }
    }

    protected void takeTablesOffline(Connector conn, List<String> tableNames) throws AccumuloSecurityException, AccumuloException, TableNotFoundException {
        for (String tableName : tableNames) {
            LOGGER.debug("taking table " + tableName + " offline");
            conn.tableOperations().offline(tableName);
        }
    }

    protected Path getTableListPath(FileSystem fileSystem, String hdfsDirectory) {
        return getPath(fileSystem, hdfsDirectory, "table-list.txt");
    }

    protected Path getPath(FileSystem fileSystem, String hdfsDirectory, String fileName) {
        if (!hdfsDirectory.startsWith("hdfs")) {
            hdfsDirectory = fileSystem.getUri() + hdfsDirectory;
        }
        return new Path(hdfsDirectory, fileName);
    }

    protected List<String> getFileLines(FileSystem fileSystem, String dir, String fileName) throws IOException {
        Path path = getPath(fileSystem, dir, fileName);
        return getFileLines(fileSystem, path);
    }

    protected List<String> getFileLines(FileSystem fileSystem, Path path) throws IOException {
        FSDataInputStream in = fileSystem.open(path);
        try {
            return IOUtils.readLines(in);
        } finally {
            in.close();
        }
    }

    protected void copyFile(FileSystem fileSystem, Path src, Path dest) throws IOException {
        FSDataInputStream in = fileSystem.open(src);
        try {
            FSDataOutputStream out = fileSystem.create(dest);
            try {
                IOUtils.copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    protected void writeFile(FileSystem fileSystem, Path path, String content) throws IOException {
        FSDataOutputStream out = fileSystem.create(path);
        try {
            out.write(content.getBytes());
        } finally {
            out.close();
        }
    }
}
