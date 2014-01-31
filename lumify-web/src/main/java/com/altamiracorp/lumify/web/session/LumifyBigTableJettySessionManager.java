package com.altamiracorp.lumify.web.session;

import com.altamiracorp.bigtable.jetty.BigTableJettySessionManager;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;

public class LumifyBigTableJettySessionManager extends BigTableJettySessionManager {
    private static final String CONFIGURATION_LOCATION = "/opt/lumify/config/";

    protected LumifyBigTableJettySessionManager() {
        super(createModelSession());
    }

    private static ModelSession createModelSession() {
        return InjectHelper.getInstance(ModelSession.class, LumifyBootstrap.bootstrapModuleMaker(Configuration.loadConfigurationFile(CONFIGURATION_LOCATION)));
    }
}
