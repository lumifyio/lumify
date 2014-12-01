package io.lumify.gpw.yarn;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyRunner;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.twill.api.AbstractTwillRunnable;

class GraphPropertyWorkerRunnable extends AbstractTwillRunnable {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphPropertyWorkerRunnable.class);

    @Override
    public void run() {
        LOGGER.info("BEGIN Run");
        try {
            GraphPropertyRunner graphPropertyRunner = prepareGraphPropertyRunner();
            graphPropertyRunner.run();
        } catch (Exception ex) {
            throw new LumifyException("GraphPropertyRunner failed", ex);
        } finally {
            LOGGER.info("END Run");
        }
    }

    private GraphPropertyRunner prepareGraphPropertyRunner() {
        UserRepository userRepository = InjectHelper.getInstance(UserRepository.class);
        GraphPropertyRunner graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
        graphPropertyRunner.prepare(userRepository.getSystemUser());
        return graphPropertyRunner;
    }
}
