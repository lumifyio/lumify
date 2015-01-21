package io.lumify.core.config;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class FileConfigurationLoader extends ConfigurationLoader {
    /**
     * !!! DO NOT DEFINE A LOGGER here. This class get loaded very early in the process and we don't want to the logger to be initialized yet **
     */
    public static final String ENV_CONFIGURATION_LOCATION = "LUMIFY_CONFIGURATION_LOCATION";
    public static final String DEFAULT_UNIX_CONFIGURATION_LOCATION = "/opt/lumify/config/";
    public static final String DEFAULT_WINDOWS_CONFIGURATION_LOCATION = "c:/opt/lumify/config/";

    public FileConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    public Configuration createConfiguration() {
        File configDirectory = getConfigurationDirectory();
        return load(configDirectory);
    }

    private File getConfigurationDirectory() {
        String configDirectory = System.getenv(ENV_CONFIGURATION_LOCATION);
        if (configDirectory == null) {
            configDirectory = ProcessUtil.isWindows() ? DEFAULT_WINDOWS_CONFIGURATION_LOCATION : DEFAULT_UNIX_CONFIGURATION_LOCATION;
        }

        if (configDirectory.startsWith("file://")) {
            configDirectory = configDirectory.substring("file://".length());
        }

        return new File(configDirectory);
    }

    public Configuration load(File configDirectory) {
        LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FileConfigurationLoader.class);

        LOGGER.debug("Attempting to load configuration from directory: %s", configDirectory);
        if (!configDirectory.exists()) {
            throw new RuntimeException("Could not find config directory: " + configDirectory);
        }

        File[] files = configDirectory.listFiles();
        if (files == null) {
            throw new RuntimeException("Could not parse directory name: " + configDirectory);
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Map<String, String> properties = new HashMap<String, String>();
        for (File f : files) {
            if (!f.getAbsolutePath().endsWith(".properties")) {
                continue;
            }
            try {
                Map<String, String> fileProperties = loadFile(f.getAbsolutePath());
                for (Map.Entry<String, String> filePropertyEntry : fileProperties.entrySet()) {
                    properties.put(filePropertyEntry.getKey(), filePropertyEntry.getValue());
                }
            } catch (IOException ex) {
                throw new RuntimeException("Could not load config file: " + f.getAbsolutePath(), ex);
            }
        }

        return new Configuration(this, properties);
    }

    private static Map<String, String> loadFile(final String fileName) throws IOException {
        LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FileConfigurationLoader.class);

        Map<String, String> results = new HashMap<String, String>();
        LOGGER.info("Loading config file: %s", fileName);
        FileInputStream in = new FileInputStream(fileName);
        try {
            Properties properties = new Properties();
            properties.load(in);
            for (Map.Entry<Object, Object> prop : properties.entrySet()) {
                String key = prop.getKey().toString();
                String value = prop.getValue().toString();
                results.put(key, value);
            }
        } catch (Exception e) {
            LOGGER.info("Could not load configuration file: %s", fileName);
        } finally {
            in.close();
        }
        return results;
    }

    @Override
    public File resolveFileName(String fileName) {
        return new File(getConfigurationDirectory(), fileName);
    }
}
