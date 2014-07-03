package io.lumify.core.config;

import java.util.Map;

public class ConfigurationHelper {
    public static org.apache.hadoop.conf.Configuration createHadoopConfigurationFromMap(Map map) {
        org.apache.hadoop.conf.Configuration configuration = new org.apache.hadoop.conf.Configuration();
        for (Object entrySetObject : map.entrySet()) {
            Map.Entry entrySet = (Map.Entry) entrySetObject;
            configuration.set("" + entrySet.getKey(), "" + entrySet.getValue());
        }
        return configuration;
    }
}
