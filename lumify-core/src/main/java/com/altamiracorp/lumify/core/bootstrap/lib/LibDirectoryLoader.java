package com.altamiracorp.lumify.core.bootstrap.lib;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;

import java.io.File;

public class LibDirectoryLoader extends LibLoader {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LibDirectoryLoader.class);

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.info("Loading libs using %s", HdfsLibCacheLoader.class.getName());

        File libDirectory = new File(configuration.get(Configuration.LIB_DIRECTORY, "/opt/lumify/lib"));
        if (!libDirectory.exists()) {
            LOGGER.warn("Skipping lib directory %s. Directory not found.", libDirectory.getAbsolutePath());
            return;
        }
        addLibDirectory(libDirectory);
    }
}
