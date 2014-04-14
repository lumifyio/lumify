package com.altamiracorp.lumify.core.bootstrap.lib;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.common.io.Files;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;

public class HdfsLibCacheLoader extends LibLoader {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(HdfsLibCacheLoader.class);

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.debug("Loading libs using %s", HdfsLibCacheLoader.class.getName());

        String hdfsLibCacheDirectory = configuration.get(Configuration.HDFS_LIB_CACHE_DIRECTORY, null);
        if (hdfsLibCacheDirectory == null) {
            LOGGER.warn("skipping HDFS libcache. Configuration parameter %s not found", Configuration.HDFS_LIB_CACHE_DIRECTORY);
            return;
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
        File libCacheDirectory = Files.createTempDir();
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

        HashSet<String> foundFiles = addFilesFromHdfs(fs, source, dest);
        removeOldFiles(dest, foundFiles);
    }

    private static HashSet<String> addFilesFromHdfs(FileSystem fs, Path source, File dest) throws IOException {
        HashSet<String> foundFiles = new HashSet<String>();
        RemoteIterator<LocatedFileStatus> sourceFiles = fs.listFiles(source, true);
        while (sourceFiles.hasNext()) {
            LocatedFileStatus sourceFile = sourceFiles.next();
            String relativePath = sourceFile.getPath().toString().substring(source.toString().length());
            foundFiles.add(relativePath);
            File destFile = new File(dest, relativePath);
            if (sourceFile.isDirectory()) {
                destFile.mkdirs();
            } else {
                fs.copyToLocalFile(sourceFile.getPath(), new Path(destFile.getAbsolutePath()));
                new File(destFile.getParent(), "." + destFile.getName() + ".crc").deleteOnExit();
            }
            destFile.deleteOnExit();
        }
        return foundFiles;
    }

    private static void removeOldFiles(File file, HashSet<String> foundFiles) {
        if (file.isHidden() || file.getAbsolutePath().startsWith(".")) {
            return;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                throw new LumifyException(String.format("Could not list files of directory %s", file.getAbsolutePath()));
            }
            for (File f : files) {
                removeOldFiles(f, foundFiles);
            }
            return;
        }

        boolean foundFile = false;
        for (String f : foundFiles) {
            if (file.getAbsolutePath().endsWith(f)) {
                foundFile = true;
                break;
            }
        }

        if (!foundFile) {
            LOGGER.info("Removing old libcache file: %s", file.getAbsolutePath());
            file.delete();
        }
    }
}
