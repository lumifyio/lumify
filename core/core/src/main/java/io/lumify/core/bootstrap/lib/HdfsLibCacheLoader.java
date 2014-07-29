package io.lumify.core.bootstrap.lib;

import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.hadoop.fs.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

public class HdfsLibCacheLoader extends LibLoader {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(HdfsLibCacheLoader.class);
    private static boolean HDFS_STREAM_HANDLER_INITIALIZED = false;

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.info("Loading libs using %s", HdfsLibCacheLoader.class.getName());

        String hdfsLibCacheDirectory = configuration.get(Configuration.HDFS_LIB_CACHE_SOURCE_DIRECTORY, null);
        if (hdfsLibCacheDirectory == null) {
            LOGGER.warn("skipping HDFS libcache. Configuration parameter %s not found", Configuration.HDFS_LIB_CACHE_SOURCE_DIRECTORY);
            return;
        }

        try {
            FileSystem hdfsFileSystem = getFileSystem(configuration);
            ensureHdfsStreamHandler(configuration);
            addFilesFromHdfs(hdfsFileSystem, new Path(hdfsLibCacheDirectory));
        } catch (Exception e) {
            throw new LumifyException(String.format("Could not add HDFS files from %s", hdfsLibCacheDirectory), e);
        }
    }

    private FileSystem getFileSystem(Configuration configuration) {
        FileSystem hdfsFileSystem;
        try {
            String hdfsRootDir = configuration.get(Configuration.HADOOP_URL);
            hdfsFileSystem = FileSystem.get(new URI(hdfsRootDir), configuration.toHadoopConfiguration(), "hadoop");
        } catch (Exception ex) {
            throw new LumifyException("Could not open HDFS file system.", ex);
        }
        return hdfsFileSystem;
    }

    private synchronized void ensureHdfsStreamHandler(Configuration lumifyConfig) {
        if (HDFS_STREAM_HANDLER_INITIALIZED) {
            return;
        }

        URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory(lumifyConfig.toHadoopConfiguration()));
        HDFS_STREAM_HANDLER_INITIALIZED = true;
    }

    private static void addFilesFromHdfs(FileSystem fs, Path source) throws IOException, NoSuchAlgorithmException {
        LOGGER.debug("adding files from HDFS path %s", source.toString());
        RemoteIterator<LocatedFileStatus> sourceFiles = fs.listFiles(source, true);
        while (sourceFiles.hasNext()) {
            LocatedFileStatus sourceFile = sourceFiles.next();
            if (sourceFile.isDirectory()) {
                continue;
            }
            addLibFile(sourceFile.getPath().toUri().toURL());
        }
    }

}
