package io.lumify.core.bootstrap.lib;

import io.lumify.core.config.Configuration;
import io.lumify.core.config.FileConfigurationLoader;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.io.File;
import java.util.List;

public class LibDirectoryLoader extends LibLoader {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LibDirectoryLoader.class);

    @Override
    public void loadLibs(Configuration configuration) {
        LOGGER.info("Loading libs using %s", LibDirectoryLoader.class.getName());
        List<File> libDirectories = FileConfigurationLoader.getLumifyDirectoriesFromMostPriority("lib");
        for (File libDirectory : libDirectories) {
            addLibDirectory(libDirectory);
        }
    }
}
