package com.altamiracorp.lumify.core.config;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for parsing application configuration file and providing
 * configuration values to the application
 */
public final class Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    public static final String APP_CONFIG_LOCATION = "application.config.location";
    public static final String APP_CREDENTIALS_LOCATION = "application.config.credentials.location";

    public static final String HADOOP_URL = "hadoop.url";
    public static final String ZK_SERVERS = "zookeeper.serverNames";
    public static final String MODEL_PROVIDER = "model.provider";
    public static final String WORK_QUEUE_REPOSITORY = "work-queue-repository";
    public static final String CONTENT_TYPE_EXTRACTOR = "content-type-extractor";
    public static final String FILESYSTEM_PROVIDER = "fs.provider";
    public static final String MODEL_USER = "model.username";
    public static final String MODEL_PASSWORD = "model.password";
    public static final String GRAPH_PROVIDER = "graph.provider";
    public static final String SEARCH_PROVIDER = "search.provider";
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

    /**
     * Attempts to parse the application configuration file located at the
     * specified filesystem path
     *
     * @param configUrl      The URL to the configuration file, not null or empty
     * @param credentialsUrl The URL to the credentials file, not null or empty
     * @return A {@link Configuration} object that contains the parsed configuration values
     */
    public static Configuration loadConfigurationFile(final String configUrl, final String credentialsUrl) {
        checkNotNull(configUrl, "The specified config file URL was null");
        checkArgument(!configUrl.isEmpty(), "The specified config file URL was empty");

        LOGGER.debug(String.format("Attempting to load configuration file: %s and credentials file: %s", configUrl, credentialsUrl));

        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
        propertiesConfiguration.setDelimiterParsingDisabled(true);
        processFile(configUrl, propertiesConfiguration);
        if (credentialsUrl != null) {
            processFile(credentialsUrl, propertiesConfiguration);
        }

        return new Configuration(propertiesConfiguration);
    }

    private static void processFile(final String fileUrl, final PropertiesConfiguration propertiesConfiguration) {
        try {
            final URL url = new URL(fileUrl);
            propertiesConfiguration.load(url.openStream());
        } catch (MalformedURLException e) {
            LOGGER.error("Could not create URL object for malformed URL: " + fileUrl, e);
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            LOGGER.info("Could not find file to load: " + fileUrl);
        } catch (IOException e) {
            LOGGER.error("Error occurred while loading file: " + fileUrl, e);
            throw new RuntimeException(e);
        } catch (ConfigurationException e) {
            LOGGER.info("Could not find file to load: " + fileUrl);
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

