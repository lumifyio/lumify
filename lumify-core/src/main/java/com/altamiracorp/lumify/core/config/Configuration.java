package com.altamiracorp.lumify.core.config;

import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for parsing application configuration file and providing
 * configuration values to the application
 */
public final class Configuration {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Configuration.class);
    public static final String HADOOP_URL = "hadoop.url";
    public static final String ZK_SERVERS = "zookeeper.serverNames";
    public static final String MODEL_PROVIDER = "model.provider";
    public static final String WORK_QUEUE_REPOSITORY = "work-queue-repository";
    public static final String CONTENT_TYPE_EXTRACTOR = "content-type-extractor";
    public static final String FILESYSTEM_PROVIDER = "fs.provider";
    public static final String MODEL_USER = "model.username";
    public static final String MODEL_PASSWORD = "model.password";
    public static final String GRAPH_PROVIDER = "graph.provider";
    public static final String AUTHENTICATION_PROVIDER = "authentication.provider";
    public static final String MAP_PROVIDER = "map.provider";
    public static final String MAP_ACCESS_KEY = "map.apiKey";
    public static final String MAP_TILE_SERVER_HOST = "map.tileServer.hostName";
    public static final String MAP_TILE_SERVER_PORT = "map.tileServer.port";
    public static final String OBJECT_DETECTOR = "object.detector";

    private org.apache.commons.configuration.Configuration config;


    /**
     * Default value for a {@link String} property that could not be parsed
     */
    private static final String UNKNOWN_STRING = "Unknown";

    /**
     * Default value for a {@link Integer} property that could not be parsed
     */
    private static final int UNKNOWN_INT = -999;

    public Configuration() {
        this.config = new PropertiesConfiguration();
        ((PropertiesConfiguration) this.config).setDelimiterParsingDisabled(true);
    }

    public Configuration(final Map<?, ?> config) {
        this();
        for (Map.Entry entry : config.entrySet()) {
            if (entry.getValue() != null) {
                set(entry.getKey().toString(), entry.getValue());
            }
        }
    }
    
    private Configuration(org.apache.commons.configuration.Configuration config) {
        this.config = config;
        if (this.config instanceof AbstractConfiguration) {
            ((AbstractConfiguration) this.config).setDelimiterParsingDisabled(true);
        }
    }

    public String get(String propertyKey) {
        return get(propertyKey, UNKNOWN_STRING);
    }

    public String get(String propertyKey, String defaultValue) {
        return config.getString(propertyKey, defaultValue);
    }

    public Integer getInt(String propertyKey) {
        return config.getInt(propertyKey, UNKNOWN_INT);
    }

    public Class getClass(String propertyKey, String defaultClassName) throws ClassNotFoundException {
        String className = config.getString(propertyKey, defaultClassName);
        if (className == null) {
            return null;
        }
        return Class.forName(className);
    }

    public Class getClass(String propertyKey) throws ClassNotFoundException {
        return getClass(propertyKey, null);
    }

    public Configuration getSubset(String keyPrefix) {
        org.apache.commons.configuration.Configuration subset = config.subset(keyPrefix);
        return new Configuration(subset);
    }

    public Map toMap() {
        return new ConfigurationMap(config);
    }

    public Iterable<String> getKeys() {
        //convenience method to easily get keys
        ConfigurationMap map = new ConfigurationMap(config);
        return map.keySet();
    }

    public void set(String propertyKey, Object value) {
        config.setProperty(propertyKey, value);
    }

    public static Configuration loadConfigurationFile(String configDirectory) {
        checkNotNull(configDirectory, "The specified config file URL was null");
        checkArgument(!configDirectory.isEmpty(), "The specified config file URL was empty");

        LOGGER.debug("Attempting to load configuration from directory: %s", configDirectory);
        if (configDirectory.startsWith("file://")) {
            configDirectory = configDirectory.substring("file://".length());
        }
        File configDirectoryFile = new File(configDirectory);
        if (!configDirectoryFile.exists()) {
            throw new RuntimeException("Could not find config directory: " + configDirectory);
        }

        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
        propertiesConfiguration.setDelimiterParsingDisabled(true);

        File[] files = configDirectoryFile.listFiles();
        if (files == null) {
            throw new RuntimeException("Could not parse directory name: " + configDirectory);
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (File f : files) {
            if (!f.getAbsolutePath().endsWith(".properties")) {
                continue;
            }
            try {
                processFile(f.getAbsolutePath(), propertiesConfiguration);
            } catch (IOException ex) {
                throw new RuntimeException("Could not load config file: " + f.getAbsolutePath(), ex);
            }
        }

        return new Configuration(propertiesConfiguration);
    }

    private static void processFile(final String fileName, final PropertiesConfiguration propertiesConfiguration) throws IOException {
        LOGGER.info("Loading config file: %s", fileName);
        FileInputStream in = new FileInputStream(fileName);
        try {
            propertiesConfiguration.load(in);
        } catch (ConfigurationException e) {
            LOGGER.info("Could not find file to load: ", fileName);
        } finally {
            in.close();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> keys = config.getKeys();
        String key;
        while (keys.hasNext()) {
            key = keys.next();
            sb.append(key).append(": ").append(config.getString(key)).append("\n");
        }

        return sb.toString();
    }
}

