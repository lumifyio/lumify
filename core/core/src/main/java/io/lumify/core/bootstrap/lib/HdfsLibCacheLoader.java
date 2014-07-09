package io.lumify.core.bootstrap.lib;

import com.google.common.io.Files;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.codec.binary.Hex;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

        if (hdfsLibCacheTempDirectory == null) {
            hdfsLibCacheTempDirectory = Files.createTempDir();
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
        LOGGER.debug("using local lib cache directory: %s", hdfsLibCacheTempDirectory.getAbsolutePath());
        hdfsLibCacheTempDirectory.mkdirs();
        return hdfsLibCacheTempDirectory;
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

    private static void syncLibCache(FileSystem fs, Path source, File destDir) throws IOException, NoSuchAlgorithmException {
        if (!fs.exists(source)) {
            throw new LumifyException(String.format("Could not sync directory %s. Directory does not exist.", source));
        }

        addFilesFromHdfs(fs, source, destDir);
    }

    private static void addFilesFromHdfs(FileSystem fs, Path source, File destDir) throws IOException, NoSuchAlgorithmException {
        LOGGER.debug("adding files from hdfs %s -> %s", source.toString(), destDir.getAbsolutePath());
        RemoteIterator<LocatedFileStatus> sourceFiles = fs.listFiles(source, true);
        while (sourceFiles.hasNext()) {
            LocatedFileStatus sourceFile = sourceFiles.next();
            if (sourceFile.isDirectory()) {
                continue;
            }
            String sourceFileRelativePath = sourceFile.getPath().toString().substring(source.toString().length());
            File tempLocalFile = copyFileLocally(fs, sourceFile);
            String md5 = calculateFileMd5(tempLocalFile);
            File newLocalFile = getLocalFileName(sourceFileRelativePath, md5, destDir);
            if (newLocalFile == null) {
                throw new LumifyException("Could not sync " + sourceFileRelativePath);
            }

            LOGGER.debug("copy to local %s -> %s", sourceFile.getPath().toString(), newLocalFile.getAbsolutePath());
            if (!newLocalFile.getParentFile().exists() && !newLocalFile.getParentFile().mkdirs()) {
                throw new LumifyException("Could not make directory for file " + newLocalFile);
            }
            if (!tempLocalFile.renameTo(newLocalFile)) {
                throw new LumifyException("Could not move file " + tempLocalFile + " to " + newLocalFile);
            }
        }
    }

    private static File getLocalFileName(String sourceFileRelativePath, String md5, File destDir) {
        int lastPeriod = sourceFileRelativePath.lastIndexOf('.');
        if (lastPeriod <= 0) {
            return null;
        }
        String relativePath = sourceFileRelativePath.substring(0, lastPeriod)
                + "-" + md5
                + sourceFileRelativePath.substring(lastPeriod);
        return new File(destDir, relativePath);
    }

    private static String calculateFileMd5(File tempLocalFile) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream in = new FileInputStream(tempLocalFile);
        try {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) > 0) {
                md.update(buffer, 0, count);
            }
            byte[] digest = md.digest();
            return new String(Hex.encodeHex(digest));
        } finally {
            in.close();
        }
    }

    private static File copyFileLocally(FileSystem fs, LocatedFileStatus sourceFile) throws IOException {
        File tempLocalFile = File.createTempFile("hdfslibcache-temp", "jar");
        fs.copyToLocalFile(sourceFile.getPath(), new Path(tempLocalFile.getAbsolutePath()));
        return tempLocalFile;
    }
}
