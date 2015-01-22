package io.lumify.core.bootstrap.lib;

import io.lumify.core.config.Configuration;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessUtil;

import java.io.File;

public class LibDirectoryLoader extends LibLoader {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LibDirectoryLoader.class);
    public static final String DEFAULT_UNIX_LIB_DIRECTORY = "/opt/lumify/lib";
    public static final String DEFAULT_WINDOWS_LIB_DIRECTORY = "c:/opt/lumify/lib";

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.info("Loading libs using %s", LibDirectoryLoader.class.getName());

        File libDirectory = new File(configuration.get(Configuration.LIB_DIRECTORY, ProcessUtil.isWindows() ? DEFAULT_WINDOWS_LIB_DIRECTORY : DEFAULT_UNIX_LIB_DIRECTORY));
        if (!libDirectory.exists()) {
            LOGGER.warn("Skipping lib directory %s. Directory not found.", libDirectory.getAbsolutePath());
            return;
        }
        addLibDirectory(libDirectory);
    }
}
