package io.lumify.core.config;

import io.lumify.test.LumifyTestCluster;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LumifyTestClusterConfigurationLoader extends ConfigurationLoader {
    private static Properties props;

    static {
        try {
            props = new Properties();
            props.load(LumifyTestClusterConfigurationLoader.class.getResourceAsStream("/io/lumify/test/lumify.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LumifyTestClusterConfigurationLoader() {
        this(new HashMap());
    }

    public LumifyTestClusterConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    @Override
    public Configuration createConfiguration() {
        return new Configuration(this, props);
    }

    @Override
    public File resolveFileName(String fileName) {
        File resourceDir = new File(LumifyTestCluster.getLumifyRootDir(), "core/test/src/main/resources/io/lumify/test");
        return new File(resourceDir, fileName);
    }

    public static Properties getConfigurationProperties() {
        return props;
    }

    public static void set(String key, String value) {
        props.put(key, value);
    }
}
