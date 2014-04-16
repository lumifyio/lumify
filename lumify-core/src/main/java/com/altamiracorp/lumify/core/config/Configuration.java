package com.altamiracorp.lumify.core.config;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.beanutils.ConvertUtilsBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for parsing application configuration file and providing
 * configuration values to the application
 */
public final class Configuration {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Configuration.class);
    public static final String CONFIGURATION_LOCATION = "/opt/lumify/config/";
    public static final String HADOOP_URL = "hadoop.url";
    public static final String ZK_SERVERS = "zookeeper.serverNames";
    public static final String MODEL_PROVIDER = "model.provider";
    public static final String WORK_QUEUE_REPOSITORY = "work-queue-repository";
    public static final String MIME_TYPE_MAPPER = "mime-type-mapper";
    public static final String FILESYSTEM_PROVIDER = "fs.provider";
    public static final String VISIBILITY_TRANSLATOR = "security.visibilityTranslator";
    public static final String GRAPH_PROVIDER = "graph";
    public static final String AUTHENTICATION_PROVIDER = "authentication.provider";
    public static final String MAP_PROVIDER = "map.provider";
    public static final String MAP_ACCESS_KEY = "map.apiKey";
    public static final String MAP_TILE_SERVER_HOST = "map.tileServer.hostName";
    public static final String MAP_TILE_SERVER_PORT = "map.tileServer.port";
    public static final String AUDIT_VISIBILITY_LABEL = "audit.visibilityLabel";
    public static final String USER_REPOSITORY = "user-repository";
    public static final String WORKSPACE_REPOSITORY = "workspace-repository";
    public static final String AUTHORIZATION_REPOSITORY = "authorization-repository";
    public static final String ONTOLOGY_REPOSITORY = "ontology-repository";
    public static final String AUDIT_REPOSITORY = "audit-repository";
    public static final String TERM_MENTION_REPOSITORY = "term-mention-repository";
    public static final String DETECTED_OBJECT_REPOSITORY = "detected-object-repository";
    public static final String ARTIFACT_THUMBNAIL_REPOSITORY = "artifact-thumbnail-repository";
    public static final String LIB_DIRECTORY = "lib-directory";
    public static final String HDFS_LIB_CACHE_DIRECTORY = "hdfs-libcache-directory";
    public static final String ONTOLOGY_REPOSITORY_OWL = "ontology-repository.owl";
    public static final String IRI_ENTITY_IMAGE = "iri.entityImage";

    private Map<String, String> config = new HashMap<String, String>();


    /**
     * Default value for a {@link String} property that could not be parsed
     */
    public static final String UNKNOWN_STRING = "Unknown";

    /**
     * Default value for a {@link Integer} property that could not be parsed
     */
    private static final int UNKNOWN_INT = -999;

    public Configuration(final Map<?, ?> config) {
        for (Map.Entry entry : config.entrySet()) {
            if (entry.getValue() != null) {
                set(entry.getKey().toString(), entry.getValue());
            }
        }
    }

    public String get(String propertyKey) {
        return get(propertyKey, UNKNOWN_STRING);
    }

    public String get(String propertyKey, String defaultValue) {
        if (config.containsKey(propertyKey)) {
            return config.get(propertyKey);
        }
        return defaultValue;
    }

    public Integer getInt(String propertyKey) {
        return Integer.parseInt(get(propertyKey, Integer.toString(UNKNOWN_INT)));
    }

    public Class getClass(String propertyKey, String defaultClassName) throws ClassNotFoundException {
        String className = get(propertyKey, defaultClassName);
        if (className == null) {
            return null;
        }
        return Class.forName(className);
    }

    public Class getClass(String propertyKey) throws ClassNotFoundException {
        return getClass(propertyKey, null);
    }

    public Map<String, String> getSubset(String keyPrefix) {
        Map<String, String> subset = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : this.config.entrySet()) {
            if (!entry.getKey().startsWith(keyPrefix + ".") && !entry.getKey().equals(keyPrefix)) {
                continue;
            }
            String newKey = entry.getKey().substring(keyPrefix.length());
            if (newKey.startsWith(".")) {
                newKey = newKey.substring(1);
            }
            subset.put(newKey, entry.getValue());
        }
        return subset;
    }

    public void setConfigurables(Object o, String keyPrefix) {
        Map<String, String> subset = getSubset(keyPrefix);
        setConfigurables(o, subset);
    }

    public void setConfigurables(Object o, Map<String, String> config) {
        ConvertUtilsBean convertUtilsBean = new ConvertUtilsBean();

        for (Method m : o.getClass().getMethods()) {
            Configurable configurableAnnotation = m.getAnnotation(Configurable.class);
            if (configurableAnnotation != null) {
                if (m.getParameterTypes().length != 1) {
                    throw new LumifyException("Invalid method to be configurable. Expected 1 argument. Found " + m.getParameterTypes().length + " arguments");
                }

                String propName = m.getName().substring("set".length());
                if (propName.length() > 1) {
                    propName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
                }

                String name;
                String defaultValue;
                if (configurableAnnotation.name() != null) {
                    name = configurableAnnotation.name();
                    defaultValue = configurableAnnotation.defaultValue();
                } else {
                    name = propName;
                    defaultValue = null;
                }
                String val;
                if (config.containsKey(name)) {
                    val = config.get(name);
                } else {
                    if ("__FAIL__".equals(defaultValue)) {
                        if (configurableAnnotation.required()) {
                            throw new LumifyException("Could not find property " + name + " for " + o.getClass().getName() + " and no default value was specified.");
                        } else {
                            continue;
                        }
                    }
                    val = defaultValue;
                }
                try {
                    Object convertedValue = convertUtilsBean.convert(val, m.getParameterTypes()[0]);
                    m.invoke(o, convertedValue);
                } catch (Exception ex) {
                    throw new LumifyException("Could not set property " + m.getName() + " on " + o.getClass().getName());
                }
            }
        }
    }

    public Map toMap() {
        return this.config;
    }

    public Iterable<String> getKeys() {
        return this.config.keySet();
    }

    public void set(String propertyKey, Object value) {
        if (value == null) {
            config.remove(propertyKey);
        } else {
            config.put(propertyKey, value.toString());
        }
    }

    public static Configuration loadConfigurationFile() {
        return loadConfigurationFile(CONFIGURATION_LOCATION);
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

        return new Configuration(properties);
    }

    private static Map<String, String> loadFile(final String fileName) throws IOException {
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        SortedSet<String> keys = new TreeSet<String>(this.config.keySet());
        for (String key : keys) {
            if (key.toLowerCase().contains("password")) {
                sb.append(key).append(": ********\n");
            } else {
                sb.append(key).append(": ").append(get(key)).append("\n");
            }
        }

        return sb.toString();
    }

    public org.apache.hadoop.conf.Configuration toHadoopConfiguration() {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        for (Object entryObj : this.toMap().entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            conf.set(entry.getKey().toString(), entry.getValue().toString());
        }
        return conf;
    }
}

