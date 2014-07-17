package io.lumify.core.config;

import java.io.File;
import java.util.Map;

public class HashMapConfigurationLoader extends ConfigurationLoader {
    public HashMapConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    @Override
    public Configuration createConfiguration() {
        return new Configuration(this, getInitParameters());
    }

    @Override
    public File resolveFileName(String fileName) {
        return new FileConfigurationLoader(getInitParameters()).resolveFileName(fileName);
    }
}
