package io.lumify.core.config;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfigurationLoader {
    public static final String ENV_CONFIGURATION_LOADER = "LUMIFY_CONFIGURATION_LOADER";
    protected static ConfigurationLoader configurationLoader;
    private final Map initParameters;

    protected ConfigurationLoader(Map initParameters) {
        this.initParameters = initParameters;
    }

    public static void configureLog4j() {
        ConfigurationLoader configurationLoader = getOrCreateConfigurationLoader();
        configurationLoader.doConfigureLog4j();
    }

    public static Configuration load() {
        return load(new HashMap());
    }

    public static Configuration load(Map p) {
        return load(getConfigurationLoaderClass(), p);
    }

    public static Class getConfigurationLoaderClass() {
        String configLoaderName = System.getenv(ENV_CONFIGURATION_LOADER);
        if (configLoaderName == null) {
            configLoaderName = System.getProperty(ENV_CONFIGURATION_LOADER);
        }
        if (configLoaderName != null) {
            return getConfigurationLoaderByName(configLoaderName);
        }

        return FileConfigurationLoader.class;
    }

    public static Configuration load(String configLoaderName, Map<String, String> initParameters) {
        Class configLoader;
        if (configLoaderName == null) {
            configLoader = getConfigurationLoaderClass();
        } else {
            configLoader = getConfigurationLoaderByName(configLoaderName);
        }
        return load(configLoader, initParameters);
    }

    public static Class getConfigurationLoaderByName(String configLoaderName) {
        Class configLoader;
        try {
            configLoader = Class.forName(configLoaderName);
        } catch (ClassNotFoundException e) {
            throw new LumifyException("Could not load class " + configLoaderName, e);
        }
        return configLoader;
    }

    public static Configuration load(Class configLoader, Map initParameters) {
        ConfigurationLoader configurationLoader = getOrCreateConfigurationLoader(configLoader, initParameters);
        return configurationLoader.createConfiguration();
    }

    private static ConfigurationLoader getOrCreateConfigurationLoader() {
        return getOrCreateConfigurationLoader(null, null);
    }

    private static ConfigurationLoader getOrCreateConfigurationLoader(Class configLoaderClass, Map initParameters) {
        if (configurationLoader != null) {
            return configurationLoader;
        }

        if (configLoaderClass == null) {
            configLoaderClass = getConfigurationLoaderClass();
        }
        if (initParameters == null) {
            initParameters = new HashMap<String, String>();
        }

        try {
            Constructor constructor = configLoaderClass.getConstructor(Map.class);
            configurationLoader = (ConfigurationLoader) constructor.newInstance(initParameters);
        } catch (Exception e) {
            throw new LumifyException("Could not load configuration class: " + configLoaderClass.getName(), e);
        }
        return configurationLoader;
    }

    public abstract Configuration createConfiguration();

    protected void doConfigureLog4j() {
        File log4jFile = resolveFileName("log4j.xml");
        if (log4jFile == null || !log4jFile.exists()) {
            throw new RuntimeException("Could not find log4j configuration at \"" + log4jFile + "\". Did you forget to copy \"docs/log4j.xml.sample\" to \"" + log4jFile + "\"");
        }
        DOMConfigurator.configure(log4jFile.getAbsolutePath());
        LumifyLogger logger = LumifyLoggerFactory.getLogger(LumifyLoggerFactory.class);
        logger.info("Using ConfigurationLoader: %s", this.getClass().getName());
        logger.info("Using log4j.xml: %s", log4jFile);
    }

    public abstract File resolveFileName(String fileName);

    protected Map getInitParameters() {
        return initParameters;
    }
}
