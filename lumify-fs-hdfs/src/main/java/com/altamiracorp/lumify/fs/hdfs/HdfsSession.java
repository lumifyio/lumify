package com.altamiracorp.lumify.fs.hdfs;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.fs.FileSystemSession;
import com.altamiracorp.lumify.core.model.SaveFileResults;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

public class HdfsSession extends FileSystemSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(HdfsSession.class);
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
            LOGGER.info("file saved: " + path);
            return new SaveFileResults(rowKey, "/data/" + rowKeyNoSpecialChars);
        } catch (IOException ex) {
            throw new RuntimeException("could not save file to HDFS", ex);
        }
    }

    @Override
    public InputStream loadFile(String path) {
        try {
            LOGGER.info("Loading file: " + path);
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
