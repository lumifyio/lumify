package io.lumify.core.bootstrap.lib;

import com.google.common.io.Files;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessUtil;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class HdfsLibCacheLoader extends LibLoader {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(HdfsLibCacheLoader.class);
    private File hdfsLibCacheTempDirectory;

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.info("Loading libs using %s", HdfsLibCacheLoader.class.getName());

        String hdfsLibCacheDirectory = configuration.get(Configuration.HDFS_LIB_CACHE_SOURCE_DIRECTORY, null);
        if (hdfsLibCacheDirectory == null) {
            LOGGER.warn("skipping HDFS libcache. Configuration parameter %s not found", Configuration.HDFS_LIB_CACHE_SOURCE_DIRECTORY);
            return;
        }

        String hdfsLibCacheTempDirectoryString = configuration.get(Configuration.HDFS_LIB_CACHE_TEMP_DIRECTORY, null);
        if (hdfsLibCacheTempDirectoryString != null) {
            hdfsLibCacheTempDirectory = new File(hdfsLibCacheTempDirectoryString);
        }

        FileSystem hdfsFileSystem = getFileSystem(configuration);
        File libCacheDirectory = ensureLocalLibCacheDirectory();

        try {
            syncLibCache(hdfsFileSystem, new Path(hdfsLibCacheDirectory), libCacheDirectory);
        } catch (Exception ex) {
            throw new LumifyException(String.format("Could not sync HDFS libcache. %s -> %s", hdfsLibCacheDirectory, libCacheDirectory.getAbsolutePath()), ex);
        }

        addLibDirectory(libCacheDirectory);
    }

    private File ensureLocalLibCacheDirectory() {
        File libCacheDirectory;
        if (hdfsLibCacheTempDirectory == null) {
            libCacheDirectory = Files.createTempDir();
        } else {
            libCacheDirectory = new File(hdfsLibCacheTempDirectory, System.getProperty("user.name") + "-" + ProcessUtil.getPid());
        }
        LOGGER.debug("using local lib cache directory: %s", libCacheDirectory.getAbsolutePath());
        libCacheDirectory.deleteOnExit();
        if (!libCacheDirectory.exists()) {
            if (!libCacheDirectory.mkdirs()) {
                throw new LumifyException("Could not mkdir " + libCacheDirectory.getAbsolutePath());
            }
        }
        return libCacheDirectory;
    }

    private FileSystem getFileSystem(Configuration configuration) {
        FileSystem hdfsFileSystem;
        try {
            String hdfsRootDir = configuration.get(Configuration.HADOOP_URL);
            org.apache.hadoop.conf.Configuration hadoopConfiguration = new org.apache.hadoop.conf.Configuration();
            hdfsFileSystem = FileSystem.get(new URI(hdfsRootDir), hadoopConfiguration, "hadoop");
        } catch (Exception ex) {
            throw new LumifyException("Could not open HDFS file system.", ex);
        }
        return hdfsFileSystem;
    }

    private static void syncLibCache(FileSystem fs, Path source, File dest) throws IOException {
        if (!fs.exists(source)) {
            throw new LumifyException(String.format("Could not sync directory %s. Directory does not exist.", source));
        }

        addFilesFromHdfs(fs, source, dest);
    }

    private static void addFilesFromHdfs(FileSystem fs, Path source, File dest) throws IOException {
        LOGGER.debug("adding files from hdfs %s -> %s", source.toString(), dest.getAbsolutePath());
        RemoteIterator<LocatedFileStatus> sourceFiles = fs.listFiles(source, true);
        while (sourceFiles.hasNext()) {
            LocatedFileStatus sourceFile = sourceFiles.next();
            String relativePath = sourceFile.getPath().toString().substring(source.toString().length());
            File destFile = new File(dest, relativePath);
            if (sourceFile.isDirectory()) {
                if (!destFile.mkdirs()) {
                    LOGGER.debug("Could not make directory %s", destFile.getAbsolutePath());
                }
            } else {
                LOGGER.debug("copy to local %s -> %s", sourceFile.getPath().toString(), destFile.getAbsolutePath());
                fs.copyToLocalFile(sourceFile.getPath(), new Path(destFile.getAbsolutePath()));
                new File(destFile.getParent(), "." + destFile.getName() + ".crc").deleteOnExit();
            }
            destFile.deleteOnExit();
        }
    }
}
