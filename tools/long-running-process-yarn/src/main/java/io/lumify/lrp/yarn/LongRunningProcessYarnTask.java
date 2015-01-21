package io.lumify.lrp.yarn;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.longRunningProcess.LongRunningProcessRunner;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.yarn.TaskBase;

public class LongRunningProcessYarnTask extends TaskBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LongRunningProcessYarnTask.class);

    public static void main(String[] args) {
        new LongRunningProcessYarnTask().run(args);
    }

    public void run() {
        LOGGER.info("BEGIN Run");
        try {
            Configuration configuration = ConfigurationLoader.load();
            InjectHelper.ModuleMaker moduleMaker = LumifyBootstrap.bootstrapModuleMaker(configuration);
            WorkQueueRepository workQueueRepository = InjectHelper.getInstance(WorkQueueRepository.class, moduleMaker, configuration);

            LongRunningProcessRunner longRunningProcessRunner = InjectHelper.getInstance(LongRunningProcessRunner.class);
            longRunningProcessRunner.prepare(configuration.toMap());

            while (true) {
                WorkQueueRepository.LongRunningProcessMessage longRunningProcessMessage = workQueueRepository.getNextLongRunningProcessMessage();
                if (longRunningProcessMessage == null) {
                    continue;
                }
                try {
                    longRunningProcessRunner.process(longRunningProcessMessage.getMessage());
                    longRunningProcessMessage.complete();
                } catch (Throwable ex) {
                    LOGGER.error("Failed to process long running process: %s", longRunningProcessMessage.getMessage());
                    longRunningProcessMessage.complete(ex);
                }
            }
        } catch (Exception ex) {
            throw new LumifyException("GraphPropertyRunner failed", ex);
        } finally {
            LOGGER.info("END Run");
        }
    }
}
