package com.altamiracorp.lumify;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;

import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapperBootstrap extends BootstrapBase {
    public MapperBootstrap(com.altamiracorp.lumify.core.config.Configuration config) {
        super(config);
    }

    public static MapperBootstrap create(final TaskInputOutputContext context) {
        //TODO: Do we need this anymore really?
        throw new RuntimeException("No!");
        /*
        checkNotNull(context);

        Configuration configuration = context.getConfiguration();
        TaskInputOutputContext attemptContext = context;

        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : configuration) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }

        return new MapperBootstrap(config);
        */
    }
}
