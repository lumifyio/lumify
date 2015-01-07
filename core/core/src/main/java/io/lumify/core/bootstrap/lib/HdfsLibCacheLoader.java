package io.lumify.core.bootstrap.lib;

import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;

public class HdfsLibCacheLoader extends LibLoader {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(HdfsLibCacheLoader.class);

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.info("Loading libs using %s", HdfsLibCacheLoader.class.getName());

        String hdfsLibCacheDirectory = configuration.get(Configuration.HDFS_LIB_CACHE_SOURCE_DIRECTORY, null);
        if (hdfsLibCacheDirectory == null) {
            LOGGER.warn("Skipping HDFS libcache. Configuration parameter %s not found", Configuration.HDFS_LIB_CACHE_SOURCE_DIRECTORY);
            return;
        }

        File libCacheDirectory = getLocalHdfsLibCacheDirectory(configuration);
        String hdfsLibCacheUser = getHdfsLibCacheUser(configuration);
        FileSystem hdfsFileSystem = getFileSystem(configuration, hdfsLibCacheUser);

        try {
            syncLibCache(hdfsFileSystem, new Path(hdfsLibCacheDirectory), libCacheDirectory);
        } catch (Exception ex) {
            throw new LumifyException(String.format("Could not sync HDFS libcache. %s -> %s", hdfsLibCacheDirectory, libCacheDirectory.getAbsolutePath()), ex);
        }
    }

    private File getLocalHdfsLibCacheDirectory(Configuration configuration) {
        String hdfsLibCacheTempDirectoryString = configuration.get(Configuration.HDFS_LIB_CACHE_TEMP_DIRECTORY, null);
        File libCacheDirectory = null;
        if (hdfsLibCacheTempDirectoryString == null) {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            libCacheDirectory = new File(baseDir, "lumify-hdfslibcache");
            LOGGER.info("Configuration parameter %s was not set; defaulting local libcache dir to %s", Configuration.HDFS_LIB_CACHE_TEMP_DIRECTORY, libCacheDirectory.getAbsolutePath());
        } else {
            libCacheDirectory = new File(hdfsLibCacheTempDirectoryString);
            LOGGER.info("Using local lib cache directory: %s", libCacheDirectory.getAbsolutePath());
        }

        if (!libCacheDirectory.exists()) {
            libCacheDirectory.mkdirs();
        }

        return libCacheDirectory;
    }

    private String getHdfsLibCacheUser(Configuration configuration) {
        String hdfsLibCacheUser = configuration.get(Configuration.HDFS_LIB_CACHE_HDFS_USER, null);
        if (hdfsLibCacheUser == null) {
            hdfsLibCacheUser = "hadoop";
            LOGGER.warn("Configuration parameter %s was not set; defaulting to HDFS user '%s'.", Configuration.HDFS_LIB_CACHE_HDFS_USER, hdfsLibCacheUser);
        } else {
            LOGGER.info("Connecting to HDFS as user '%s'", hdfsLibCacheUser);
        }
        return hdfsLibCacheUser;
    }

    private FileSystem getFileSystem(Configuration configuration, String user) {
        try {
            String hdfsRootDir = configuration.get(Configuration.HADOOP_URL, null);
            if (hdfsRootDir == null) {
                throw new LumifyException("Could not find configuration: " + Configuration.HADOOP_URL);
            }
            return FileSystem.get(new URI(hdfsRootDir), configuration.toHadoopConfiguration(), user);
        } catch (Exception ex) {
            throw new LumifyException("Could not open HDFS file system.", ex);
        }
    }

    private static void syncLibCache(FileSystem fs, Path source, File destDir) throws IOException, NoSuchAlgorithmException {
        if (!fs.exists(source)) {
            throw new LumifyException(String.format("Could not sync HDFS directory %s. Directory does not exist.", source));
        }

        addFilesFromHdfs(fs, source, destDir);
    }

    private static void addFilesFromHdfs(FileSystem fs, Path source, File destDir) throws IOException, NoSuchAlgorithmException {
        LOGGER.debug("Adding files from HDFS %s -> %s", source.toString(), destDir.getAbsolutePath());
        RemoteIterator<LocatedFileStatus> hdfsFiles = fs.listFiles(source, true);
        while (hdfsFiles.hasNext()) {
            LocatedFileStatus hdfsFile = hdfsFiles.next();
            if (hdfsFile.isDirectory()) {
                continue;
            }

            File locallyCachedFile = getLocalCacheFileName(hdfsFile, destDir);
            if (locallyCachedFile.exists()) {
                LOGGER.info("HDFS file %s already cached at %s. Skipping sync.", hdfsFile.getPath().toString(), locallyCachedFile.getPath());
            } else {
                fs.copyToLocalFile(hdfsFile.getPath(), new Path(locallyCachedFile.getAbsolutePath()));
                locallyCachedFile.setLastModified(hdfsFile.getModificationTime());
                LOGGER.info("Caching HDFS file %s -> %s", hdfsFile.getPath().toString(), locallyCachedFile.getPath());
            }

            addLibFile(locallyCachedFile);
        }
    }

    private static File getLocalCacheFileName(LocatedFileStatus hdfsFile, File destdir) {
        String filename = hdfsFile.getPath().getName();
        String baseFilename = filename.substring(0, filename.lastIndexOf('.'));
        String extension = filename.substring(filename.lastIndexOf('.'));
        String cacheFilename = baseFilename + "-" + hdfsFile.getModificationTime() + extension;
        return new File(destdir, cacheFilename);
    }

}