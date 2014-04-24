package io.lumify.fs.hdfs;

import io.lumify.core.config.Configuration;
import io.lumify.core.fs.FileSystemSession;
import io.lumify.core.model.SaveFileResults;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.RowKeyHelper;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

public class HdfsSession implements FileSystemSession {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(HdfsSession.class);
    private final FileSystem hdfsFileSystem;
    private final String hdfsRootDir;

    public HdfsSession(Configuration config) {
        try {
            hdfsRootDir = config.get(Configuration.HADOOP_URL);
            org.apache.hadoop.conf.Configuration hadoopConfiguration = new org.apache.hadoop.conf.Configuration();
            hdfsFileSystem = FileSystem.get(new URI(hdfsRootDir), hadoopConfiguration, "hadoop");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveFile(String path, InputStream in) {
        try {
            FSDataOutputStream hdfsOut = hdfsFileSystem.create(new Path(path));
            try {
                IOUtils.copyLarge(in, hdfsOut);
            } finally {
                hdfsOut.close();
            }
            LOGGER.info("file saved: %s", path);
        } catch (IOException ioe) {
            throw new RuntimeException("could not save file to HDFS", ioe);
        }
    }

    @Override
    public SaveFileResults saveFile(InputStream in) {
        try {
            String dataRoot = hdfsRootDir + "/data/";
            FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.ALL);
            if (!this.hdfsFileSystem.exists(new Path(dataRoot))) {
                this.hdfsFileSystem.mkdirs(new Path(dataRoot), fsPermission);
            }

            String tempRoot = hdfsRootDir + "/temp/";
            if (!this.hdfsFileSystem.exists(new Path(tempRoot))) {
                this.hdfsFileSystem.mkdirs(new Path(tempRoot), fsPermission);
            }

            String tempPath = tempRoot + UUID.randomUUID().toString();
            FSDataOutputStream out = this.hdfsFileSystem.create(new Path(tempPath));
            String rowKey;
            try {
                rowKey = RowKeyHelper.buildSHA256KeyString(in, out);
            } finally {
                out.close();
            }

            String rowKeyNoSpecialChars = rowKey.replaceAll("" + RowKeyHelper.MINOR_FIELD_SEPARATOR, "").replaceAll("\\x1F", "");
            String path = dataRoot + rowKeyNoSpecialChars;
            this.hdfsFileSystem.rename(new Path(tempPath), new Path(path));
            LOGGER.info("file saved: %s", path);
            return new SaveFileResults(rowKey, "/data/" + rowKeyNoSpecialChars);
        } catch (IOException ex) {
            throw new RuntimeException("could not save file to HDFS", ex);
        }
    }

    @Override
    public InputStream loadFile(String path) {
        try {
            LOGGER.info("Loading file: %s", path);
            return this.hdfsFileSystem.open(new Path(path));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public long getFileLength(String path) {
        try {
            return this.hdfsFileSystem.getFileStatus(new Path(path)).getLen();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
