package io.lumify.backupRestore;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.hadoop.fs.FileSystem;
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
        List<String> tableNames = getTableList(fileSystem, restoreOptions.getHdfsRestoreDirectory());
        restoreTables(conn, tableNames, restoreOptions.getHdfsRestoreDirectory());

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
}
