package io.lumify.backupRestore;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class Restore extends BackupRestoreBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(Restore.class);

    public void run(RestoreOptions restoreOptions) throws AccumuloSecurityException, AccumuloException, IOException, TableExistsException, URISyntaxException, InterruptedException {
        LOGGER.info("Begin restore");

        Connector conn = createAccumuloConnection(restoreOptions);
        FileSystem fileSystem = getHdfsFileSystem(restoreOptions);

        if (restoreOptions.getHdfsRestoreTempDirectory() != null) {
            // TODO: exclude the securegraphHdfsOverflowDirectory
            LOGGER.info("Copying backup files from restore directory: " + restoreOptions.getHdfsRestoreDirectory() + " to temp directory: " + restoreOptions.getHdfsRestoreTempDirectory());
            FileUtil.copy(fileSystem, new Path(restoreOptions.getHdfsRestoreDirectory()), fileSystem, new Path(restoreOptions.getHdfsRestoreTempDirectory()), false, fileSystem.getConf());

            List<String> tableNames = getTableList(fileSystem, restoreOptions.getHdfsRestoreTempDirectory());
            restoreTables(conn, tableNames, restoreOptions.getHdfsRestoreTempDirectory());

            LOGGER.info("Deleting restored temp directory: " + restoreOptions.getHdfsRestoreTempDirectory());
            fileSystem.delete(new Path(restoreOptions.getHdfsRestoreTempDirectory()), true);
        } else {
            List<String> tableNames = getTableList(fileSystem, restoreOptions.getHdfsRestoreDirectory());
            restoreTables(conn, tableNames, restoreOptions.getHdfsRestoreDirectory());

            LOGGER.warn("Deleting restored and consumed restore directory: " + restoreOptions.getHdfsRestoreDirectory());
            fileSystem.delete(new Path(restoreOptions.getHdfsRestoreDirectory()), true);
        }

        restoreSecuregraphHdfsOverflowDirectory(fileSystem, restoreOptions.getHdfsRestoreDirectory(), restoreOptions.getSecuregraphHdfsOverflowDirectory());

        LOGGER.info("Restore complete");
    }

    private void restoreTables(Connector conn, List<String> tableNames, String hdfsRestoreDirectory) throws TableExistsException, AccumuloSecurityException, AccumuloException {
        if (!hdfsRestoreDirectory.endsWith("/")) {
            hdfsRestoreDirectory = hdfsRestoreDirectory + "/";
        }

        for (String tableName : tableNames) {
            String importDir = hdfsRestoreDirectory + tableName;
            LOGGER.info("Restoring table " + tableName + " from " + importDir);
            conn.tableOperations().importTable(tableName, importDir);
        }
    }

    private List<String> getTableList(FileSystem fileSystem, String hdfsRestoreDirectory) throws IOException {
        return getFileLines(fileSystem, getTableListPath(fileSystem, hdfsRestoreDirectory));
    }

    private void restoreSecuregraphHdfsOverflowDirectory(FileSystem fileSystem, String hdfsRestoreDirectory, String securegraphHdfsOverflowDirectory) throws IOException {
        // e.g. /lumify/secureGraph
        Path destPath = new Path(securegraphHdfsOverflowDirectory);
        // e.g. /backup/yesterday + / + secureGraph + / + *
        Path srcPath = new Path(hdfsRestoreDirectory + Path.SEPARATOR + destPath.getName() + Path.SEPARATOR + "*");

        if (fileSystem.isDirectory(srcPath.getParent())) {
            LOGGER.info("restoring securegraph overflow directory from: " + srcPath.toUri() + " to: " + destPath.toUri());
            FileStatus[] fileStatuses = fileSystem.globStatus(srcPath);
            LOGGER.debug("restoring " + fileStatuses.length + " files and/or subdirectories");
            for (FileStatus fileStatus : fileStatuses) {
               FileUtil.copy(fileSystem, fileStatus.getPath(), fileSystem, destPath, false, fileSystem.getConf());
            }
        } else {
            LOGGER.warn("backup securegraph overflow directory: " + srcPath.getParent().toUri() + " not found");
        }
    }
}
