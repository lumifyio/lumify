package io.lumify.gpw.yarn;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyRunner;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

public class GraphPropertyWorkerYarnTask {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphPropertyWorkerYarnTask.class);

    public static void main(String[] args) {
        new GraphPropertyWorkerYarnTask().run(args);
    }

    public void run(String[] args) {
        LOGGER.info("BEGIN Run");
        try {
            Configuration configuration = ConfigurationLoader.load();
            InjectHelper.ModuleMaker moduleMaker = LumifyBootstrap.bootstrapModuleMaker(configuration);
            UserRepository userRepository = InjectHelper.getInstance(UserRepository.class, moduleMaker);

            GraphPropertyRunner graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
            graphPropertyRunner.prepare(userRepository.getSystemUser());
            graphPropertyRunner.run();
        } catch (Exception ex) {
            throw new LumifyException("GraphPropertyRunner failed", ex);
        } finally {
            LOGGER.info("END Run");
        }
    }
}
