package com.altamiracorp.lumify.core.cmdline;

import com.altamiracorp.lumify.core.BootstrapBase;
import com.altamiracorp.lumify.core.config.Configuration;

public class CommandLineBootstrap extends BootstrapBase {
    protected CommandLineBootstrap(Configuration config) {
        super(config);
    }

    public static CommandLineBootstrap create(Configuration config) {
        return new CommandLineBootstrap(config);
    }
}
