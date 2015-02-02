package io.lumify.backupRestore;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Backup extends BackupRestoreBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(Backup.class);

    public void run(BackupOptions backupOptions) throws Exception {
        LOGGER.info("Begin backup");

        Connector conn = createAccumuloConnection(backupOptions);
        FileSystem fileSystem = getHdfsFileSystem(backupOptions);

        List<String> tablesToBackup = getTablesToBackup(conn, backupOptions.getTableNamePrefix());
        try {
            takeTablesOffline(conn, tablesToBackup);
            backupTables(conn, fileSystem, tablesToBackup, backupOptions.getHdfsBackupDirectory());
            saveTablesList(tablesToBackup, fileSystem, backupOptions.getHdfsBackupDirectory());
            backupSecuregraphHdfsOverflowDirectory(fileSystem, backupOptions.getSecuregraphHdfsOverflowDirectory(), backupOptions.getHdfsBackupDirectory());
        } finally {
            takeTableOnline(conn, tablesToBackup);
        }

        LOGGER.info("Backup complete");
    }

    private void saveTablesList(List<String> tableNames, FileSystem fileSystem, String hdfsBackupDirectory) throws IOException {
        Path path = getTableListPath(fileSystem, hdfsBackupDirectory);
        FSDataOutputStream out = fileSystem.create(path);
        for (String tableName : tableNames) {
            out.write(tableName.getBytes());
            out.write("\n".getBytes());
        }
        out.close();
    }

    private void backupTables(Connector conn, FileSystem fileSystem, List<String> tableNames, String backupDirectory) throws AccumuloSecurityException, TableNotFoundException, AccumuloException, IOException {
        for (String tableName : tableNames) {
            String dir = backupDirectory + "/" + tableName;
            backupTable(conn, fileSystem, tableName, dir);
        }
    }

    private void backupTable(Connector conn, FileSystem fileSystem, String tableName, String dir) throws TableNotFoundException, AccumuloException, AccumuloSecurityException, IOException {
        LOGGER.debug("backing up table " + tableName + " to " + dir);
        conn.tableOperations().exportTable(tableName, dir);

        StringBuilder newDistcp = new StringBuilder();
        Path distcpPath = getPath(fileSystem, dir, "distcp.txt");
        List<String> distcp = getFileLines(fileSystem, distcpPath);
        for (String file : distcp) {
            file = file.trim();
            if (file.length() == 0) {
                continue;
            }
            if (file.endsWith("exportMetadata.zip")) {
                newDistcp.append(file);
                newDistcp.append("\n");
                continue;
            }
            Path src = new Path(file);
            Path dest = getPath(fileSystem, dir, src.getName());
            copyFile(fileSystem, src, dest);
            newDistcp.append(dest.toUri().toString());
            newDistcp.append("\n");
        }

        writeFile(fileSystem, distcpPath, newDistcp.toString());
    }

    private List<String> getTablesToBackup(Connector conn, String tableNamePrefix) {
        ArrayList<String> results = new ArrayList<String>();
        for (String tableName : conn.tableOperations().list()) {
            if (tableName.startsWith(tableNamePrefix)) {
                results.add(tableName);
            }
        }
        return results;
    }

    private void backupSecuregraphHdfsOverflowDirectory(FileSystem fileSystem, String securegraphHdfsOverflowDirectory, String hdfsBackupDirectory) throws IOException {
        Path srcPath = new Path(securegraphHdfsOverflowDirectory);
        Path destPath = new Path(hdfsBackupDirectory);

        if (fileSystem.isDirectory(srcPath)) {
            LOGGER.info("backing up securegraph overflow directory from: " + srcPath.toUri() + " to: " + destPath.toUri());
            FileUtil.copy(fileSystem, srcPath, fileSystem, destPath, false, fileSystem.getConf());
        } else {
            LOGGER.warn("securegraph overflow directory: " + srcPath.toUri() + " not found");
        }
    }
}
