package com.altamiracorp.lumify.core.storm;

import com.altamiracorp.lumify.core.BootstrapBase;
import com.altamiracorp.lumify.core.config.Configuration;
import com.google.inject.Module;

import java.util.Map;

public class StormBootstrap extends BootstrapBase {
    protected StormBootstrap(Configuration config) {
        super(config);
    }

    public static Module create(Map stormConf) {
        Configuration config = new Configuration();
        for (Object entryObj : stormConf.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            if (entry.getValue() != null) {
                config.set((String) entry.getKey(), entry.getValue());
            }
        }
        return new StormBootstrap(config);
    }
}
