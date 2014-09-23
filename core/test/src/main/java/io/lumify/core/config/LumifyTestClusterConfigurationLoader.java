package io.lumify.core.config;

import io.lumify.test.LumifyTestCluster;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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
        try {
            URL rootUrl = LumifyTestCluster.class.getResource(fileName);
            if (rootUrl == null || rootUrl.toURI() == null) {
                throw new RuntimeException("Could not resolve file: " + fileName);
            }
            return new File(rootUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Properties getConfigurationProperties() {
        return props;
    }
}
