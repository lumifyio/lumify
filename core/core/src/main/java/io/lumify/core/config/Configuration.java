package io.lumify.core.config;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.ClassUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.beanutils.ConvertUtilsBean;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Responsible for parsing application configuration file and providing
 * configuration values to the application
 */
public final class Configuration {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Configuration.class);
    public static final String HADOOP_URL = "hadoop.url";
    public static final String HDFS_LIB_CACHE_SOURCE_DIRECTORY = "hdfsLibcache.sourceDirectory";
    public static final String HDFS_LIB_CACHE_TEMP_DIRECTORY = "hdfsLibcache.tempDirectory";
    public static final String LIB_DIRECTORY = "lib-directory";
    public static final String ZK_SERVERS = "zookeeper.serverNames";
    public static final String MODEL_PROVIDER = "model.provider";
    public static final String FILESYSTEM_PROVIDER = "fs.provider";
    public static final String AUTHENTICATION_PROVIDER = "authentication.provider";
    public static final String MAP_PROVIDER = "map.provider";
    public static final String MAP_ACCESS_KEY = "map.apiKey";
    public static final String MAP_TILE_SERVER_HOST = "map.tileServer.hostName";
    public static final String MAP_TILE_SERVER_PORT = "map.tileServer.port";
    public static final String USER_REPOSITORY = "repository.user";
    public static final String WORKSPACE_REPOSITORY = "repository.workspace";
    public static final String AUTHORIZATION_REPOSITORY = "repository.authorization";
    public static final String ONTOLOGY_REPOSITORY = "repository.ontology";
    public static final String AUDIT_REPOSITORY = "repository.audit";
    public static final String TERM_MENTION_REPOSITORY = "repository.termMention";
    public static final String DETECTED_OBJECT_REPOSITORY = "repository.detectedObject";
    public static final String ARTIFACT_THUMBNAIL_REPOSITORY = "repository.artifactThumbnail";
    public static final String WORK_QUEUE_REPOSITORY = "repository.workQueue";
    public static final String ONTOLOGY_REPOSITORY_OWL = "repository.ontology.owl";
    public static final String ONTOLOGY_IRI_PREFIX = "ontology.iri.";
    public static final String ONTOLOGY_IRI_ENTITY_IMAGE = ONTOLOGY_IRI_PREFIX + "entityImage";
    public static final String ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY = ONTOLOGY_IRI_PREFIX + "artifactHasEntity";
    public static final String ONTOLOGY_IRI_ENTITY_HAS_IMAGE = ONTOLOGY_IRI_PREFIX + "entityHasImage";
    public static final String ONTOLOGY_IRI_ARTIFACT_CONTAINS_IMAGE_OF_ENTITY = ONTOLOGY_IRI_PREFIX + "artifactContainsImageOfEntity";
    public static final String GRAPH_PROVIDER = "graph";
    public static final String VISIBILITY_TRANSLATOR = "security.visibilityTranslator";
    public static final String AUDIT_VISIBILITY_LABEL = "audit.visibilityLabel";
    public static final String DEFAULT_PRIVILEGES = "newuser.privileges";
    public static final String WEB_PROPERTIES_PREFIX = "web.ui.";
    public static final String WEB_GEOCODER_ENABLED = WEB_PROPERTIES_PREFIX + "geocoder.enabled";
    private final ConfigurationLoader configurationLoader;

    private Map<String, String> config = new HashMap<String, String>();

    /**
     * Default value for a {@link String} property that could not be parsed
     */
    public static final String UNKNOWN_STRING = "Unknown";

    Configuration(final ConfigurationLoader configurationLoader, final Map<?, ?> config) {
        this.configurationLoader = configurationLoader;
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
        return config.containsKey(propertyKey) ? config.get(propertyKey) : defaultValue;
    }

    public Integer getInt(String propertyKey, Integer defaultValue) {
        return Integer.parseInt(get(propertyKey, defaultValue == null ? null : defaultValue.toString()));
    }

    public Integer getInt(String propertyKey) {
        return getInt(propertyKey, null);
    }

    public <T> Class<? extends T> getClass(String propertyKey) {
        String className = get(propertyKey, null);
        if (className == null) {
            throw new LumifyException("Could not find required property " + propertyKey);
        }
        try {
            LOGGER.debug("found class \"%s\" for configuration \"%s\"", className, propertyKey);
            return ClassUtil.forName(className);
        } catch (LumifyException e) {
            throw new LumifyException("Could not load class " + className + " for property " + propertyKey, e);
        }
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
                    if (Configurable.DEFAULT_VALUE.equals(defaultValue)) {
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

    public File resolveFileName(String fileName) {
        return this.configurationLoader.resolveFileName(fileName);
    }
}

