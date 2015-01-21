package io.lumify.web.session;

import com.altamiracorp.bigtable.jetty.BigTableJettySessionManager;
import com.altamiracorp.bigtable.model.ModelSession;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;

public class LumifyBigTableJettySessionManager extends BigTableJettySessionManager {
    public LumifyBigTableJettySessionManager() {
        super(createModelSession());
    }

    private static ModelSession createModelSession() {
        Configuration configuration = ConfigurationLoader.load();
        return InjectHelper.getInstance(ModelSession.class, LumifyBootstrap.bootstrapModuleMaker(configuration), configuration);
    }
}
