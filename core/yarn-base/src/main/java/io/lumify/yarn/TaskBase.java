package io.lumify.yarn;

import com.beust.jcommander.JCommander;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.hadoop.yarn.api.records.ContainerExitStatus;

public abstract class TaskBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TaskBase.class);

    public final void run(String[] args) {
        new JCommander(this, args);
        try {
            LOGGER.info("BEGIN Run");
            run();
            LOGGER.info("END Run");
            System.exit(ContainerExitStatus.SUCCESS);
        } catch (Throwable ex) {
            LOGGER.info("FAILED Run", ex);
            System.exit(1);
        }
    }

    protected abstract void run();
}
