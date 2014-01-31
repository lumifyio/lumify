package com.altamiracorp.lumify.web.session;

import com.altamiracorp.bigtable.jetty.BigTableJettySessionManager;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;
import com.google.inject.Inject;

public class LumifyBigTableJettySessionManager extends BigTableJettySessionManager {
    private static final String CONFIGURATION_LOCATION = "/opt/lumify/config/";

    protected LumifyBigTableJettySessionManager() {
        super(createModelSession());
    }

    private static ModelSession createModelSession() {
        ModelSessionContainer modelSessionContainer = new ModelSessionContainer();
        InjectHelper.inject(modelSessionContainer, LumifyBootstrap.bootstrapModuleMaker(Configuration.loadConfigurationFile(CONFIGURATION_LOCATION)));
        return modelSessionContainer.getModelSession();
    }

    private static class ModelSessionContainer {
        private ModelSession modelSession;

        public ModelSession getModelSession() {
            return modelSession;
        }

        @Inject
        public void setModelSession(ModelSession modelSession) {
            this.modelSession = modelSession;
        }
    }
}
