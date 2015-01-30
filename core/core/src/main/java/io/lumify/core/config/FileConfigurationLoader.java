package io.lumify.core.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Searches for lumify configuration directories in this order:
 * - ${ENV_LUMIFY_DIR}
 * - ${user.home}/.lumify
 * - ${appdata}/Lumify
 * - DEFAULT_UNIX_LOCATION or DEFAULT_WINDOWS_LOCATION
 */
public class FileConfigurationLoader extends ConfigurationLoader {
    /**
     * !!! DO NOT DEFINE A LOGGER here. This class get loaded very early in the process and we don't want to the logger to be initialized yet **
     */
    public static final String ENV_LUMIFY_DIR = "LUMIFY_DIR";
    public static final String DEFAULT_UNIX_LOCATION = "/opt/lumify/";
    public static final String DEFAULT_WINDOWS_LOCATION = "c:/opt/lumify/";

    public FileConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    public Configuration createConfiguration() {
        final Map<String, String> properties = new HashMap<>();
        List<File> configDirectories = getLumifyDirectoriesFromLeastPriority("config");
        for (File directory : configDirectories) {
            Map<String, String> directoryProperties = loadDirectory(directory);
            properties.putAll(directoryProperties);
        }
        return new Configuration(this, properties);
    }

    public static List<File> getLumifyDirectoriesFromMostPriority(String subDirectory) {
        return Lists.reverse(getLumifyDirectoriesFromLeastPriority(subDirectory));
    }

    public static List<File> getLumifyDirectoriesFromLeastPriority(String subDirectory) {
        List<File> results = new ArrayList<>();

        if (ProcessUtil.isWindows()) {
            addLumifySubDirectory(results, DEFAULT_WINDOWS_LOCATION, subDirectory);
        } else {
            addLumifySubDirectory(results, DEFAULT_UNIX_LOCATION, subDirectory);
        }

        String appData = System.getProperty("appdata");
        if (appData != null && appData.length() > 0) {
            addLumifySubDirectory(results, new File(new File(appData), "Lumify").getAbsolutePath(), subDirectory);
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && userHome.length() > 0) {
            addLumifySubDirectory(results, new File(new File(userHome), ".lumify").getAbsolutePath(), subDirectory);
        }

        addLumifySubDirectory(results, System.getenv(ENV_LUMIFY_DIR), subDirectory);

        if (results.size() == 0) {
            throw new LumifyException("Could not find any valid config directories.");
        }

        return ImmutableList.copyOf(results);
    }

    private static void addLumifySubDirectory(List<File> results, String location, String subDirectory) {
        if (location == null || location.trim().length() == 0) {
            return;
        }

        location = location.trim();
        if (location.startsWith("file://")) {
            location = location.substring("file://".length());
        }

        File dir = new File(new File(location), subDirectory);
        if (!dir.exists()) {
            return;
        }

        results.add(dir);
    }

    private static Map<String, String> loadDirectory(File configDirectory) {
        LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FileConfigurationLoader.class);

        LOGGER.debug("Attempting to load configuration from directory: %s", configDirectory);
        if (!configDirectory.exists()) {
            throw new LumifyException("Could not find config directory: " + configDirectory);
        }

        File[] files = configDirectory.listFiles();
        if (files == null) {
            throw new LumifyException("Could not parse directory name: " + configDirectory);
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Map<String, String> properties = new HashMap<>();
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
                throw new LumifyException("Could not load config file: " + f.getAbsolutePath(), ex);
            }
        }

        return properties;
    }

    private static Map<String, String> loadFile(final String fileName) throws IOException {
        LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FileConfigurationLoader.class);

        Map<String, String> results = new HashMap<>();
        LOGGER.info("Loading config file: %s", fileName);
        try (FileInputStream in = new FileInputStream(fileName)) {
            Properties properties = new Properties();
            properties.load(in);
            for (Map.Entry<Object, Object> prop : properties.entrySet()) {
                String key = prop.getKey().toString();
                String value = prop.getValue().toString();
                results.put(key, value);
            }
        } catch (Exception e) {
            LOGGER.info("Could not load configuration file: %s", fileName);
        }
        return results;
    }

    @Override
    public File resolveFileName(String fileName) {
        List<File> configDirectories = getLumifyDirectoriesFromMostPriority("config");
        for (File directory : configDirectories) {
            File f = new File(directory, fileName);
            if (f.exists()) {
                return f;
            }
        }
        return new File(configDirectories.get(0), fileName);
    }
}
