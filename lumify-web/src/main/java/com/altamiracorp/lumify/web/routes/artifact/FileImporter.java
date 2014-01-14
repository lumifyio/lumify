package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.inject.Inject;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class FileImporter {
    private static final String UNKNOWN_DATA_DIR = "/lumify/data/unknown";
    private static final String TEMP_DIR = "/lumify/data/tmp";
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FileImporter.class);
    private final FileSystem hdfsFileSystem;

    @Inject
    public FileImporter(Configuration config) {
        try {
            String hdfsRootDir = config.get(Configuration.HADOOP_URL);
            org.apache.hadoop.conf.Configuration hadoopConfiguration = new org.apache.hadoop.conf.Configuration();
            hdfsFileSystem = FileSystem.get(new URI(hdfsRootDir), hadoopConfiguration, "hadoop");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeFile(InputStream fileStream, String uploadFileName) throws IOException, ArchiveException {
        checkNotNull(fileStream);
        checkNotNull(uploadFileName);
        checkArgument(!uploadFileName.isEmpty());

        if (FileUtils.getExtension(uploadFileName).equals("tar") || FileUtils.getExtension(uploadFileName).equals("tgz")) {
            TarArchiveInputStream archiveInputStream;
            if (FileUtils.getExtension(uploadFileName).equals("tar")) {
                archiveInputStream = new TarArchiveInputStream(fileStream);
            } else {
                archiveInputStream = new TarArchiveInputStream(new GZIPInputStream(fileStream));
            }
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) archiveInputStream.getNextEntry()) != null) {
                String fileName = FileUtils.basename(entry.getName());
                if (entry.isDirectory()) {
                    for (TarArchiveEntry child : entry.getDirectoryEntries()) {
                        String basename = FileUtils.basename(child.getName());
                        copyToHdfs(archiveInputStream, basename);
                    }
                } else {
                    copyToHdfs(archiveInputStream, fileName);
                }
            }
        } else {
            copyToHdfs(fileStream, uploadFileName);
        }
        fileStream.close();
    }

    private void copyToHdfs(InputStream stream, String filename) throws IOException {
        if (filename.startsWith(".")) {
            return;
        }

        final File outputFile = File.createTempFile("fileImport", "bin");

        LOGGER.debug("Writing stream to temporary file location: %s", outputFile.getAbsolutePath());
        FileOutputStream out = new FileOutputStream(outputFile);
        try {
            IOUtils.copy(stream, out);
        } finally {
            out.close();
        }

        hdfsFileSystem.copyFromLocalFile(true, true, new Path(outputFile.getAbsolutePath()), new Path(TEMP_DIR, filename));
        hdfsFileSystem.rename(new Path(TEMP_DIR + "/" + filename), new Path(UNKNOWN_DATA_DIR + "/" + filename));
        LOGGER.info("Uploaded file written to distributed filesystem");
    }
}
