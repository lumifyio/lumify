package io.lumify.core.model.longRunningProcess;

import com.google.inject.Inject;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LongRunningProcessRunner {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LongRunningProcessRunner.class);
    private UserRepository userRepository;
    private LongRunningProcessRepository longRunningProcessRepository;
    private WorkQueueRepository workQueueRepository;
    private User user;
    private List<LongRunningProcessWorker> workers = new ArrayList<LongRunningProcessWorker>();

    public void prepare(Map map) {
        prepareUser(map);
        prepareWorkers(map);
    }

    private void prepareUser(Map map) {
        this.user = (User) map.get("user");
        if (this.user == null) {
            this.user = this.userRepository.getSystemUser();
        }
    }

    private void prepareWorkers(Map map) {
        LongRunningWorkerPrepareData workerPrepareData = new LongRunningWorkerPrepareData(
                map,
                this.user,
                InjectHelper.getInjector());
        for (LongRunningProcessWorker worker : InjectHelper.getInjectedServices(LongRunningProcessWorker.class)) {
            try {
                worker.prepare(workerPrepareData);
            } catch (Exception ex) {
                throw new LumifyException("Could not prepare graph property worker " + worker.getClass().getName(), ex);
            }
            workers.add(worker);
        }
    }

    public void process(JSONObject longRunningProcessQueueItem) {
        LOGGER.info("process long running queue item %s", longRunningProcessQueueItem.toString());

        for (LongRunningProcessWorker worker : workers) {
            if (worker.isHandled(longRunningProcessQueueItem)) {
                try {
                    longRunningProcessQueueItem.put("startTime", System.currentTimeMillis());
                    longRunningProcessQueueItem.put("progress", 0.0);
                    longRunningProcessRepository.beginWork(longRunningProcessQueueItem);
                    workQueueRepository.broadcastLongRunningProcessChange(longRunningProcessQueueItem);

                    worker.process(longRunningProcessQueueItem);

                    longRunningProcessQueueItem.put("endTime", System.currentTimeMillis());
                    longRunningProcessQueueItem.put("progress", 1.0);
                    longRunningProcessRepository.ack(longRunningProcessQueueItem);
                    workQueueRepository.broadcastLongRunningProcessChange(longRunningProcessQueueItem);
                } catch (Throwable ex) {
                    LOGGER.error("Failed to process long running process queue item", ex);
                    longRunningProcessQueueItem.put("error", ex.getMessage());
                    longRunningProcessQueueItem.put("endTime", System.currentTimeMillis());
                    longRunningProcessRepository.nak(longRunningProcessQueueItem, ex);
                    workQueueRepository.broadcastLongRunningProcessChange(longRunningProcessQueueItem);
                }
                return;
            }
        }
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public void setLongRunningProcessRepository(LongRunningProcessRepository longRunningProcessRepository) {
        this.longRunningProcessRepository = longRunningProcessRepository;
    }
}
