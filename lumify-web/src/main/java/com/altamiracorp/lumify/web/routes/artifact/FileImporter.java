package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class FileImporter {
    private static final String UNKNOWN_DATA_DIR = "/lumify/data/unknown";
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

    public void writeFile(InputStream fileStream, String uploadFileName) throws IOException {
        checkNotNull(fileStream);
        checkNotNull(uploadFileName);
        checkArgument(!uploadFileName.isEmpty());

        final File tempFile = File.createTempFile("fileImport", "bin");
        LOGGER.debug("Writing stream to temporary file location: %s", tempFile.getAbsolutePath());
        writeToTempFile(fileStream, tempFile);

        hdfsFileSystem.copyFromLocalFile(true, true, new Path(tempFile.getAbsolutePath()), new Path(UNKNOWN_DATA_DIR, uploadFileName));
        LOGGER.info("Uploaded file written to distributed filesystem");
    }

    private void writeToTempFile(InputStream in, File tempFile) throws IOException {
        try {
            FileOutputStream out = new FileOutputStream(tempFile);
            try {
                IOUtils.copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
