package io.lumify.core.model.longRunningProcess;

import org.json.JSONObject;

public abstract class LongRunningProcessWorker {
    public void prepare(LongRunningWorkerPrepareData workerPrepareData) {

    }

    public abstract boolean isHandled(JSONObject longRunningProcessQueueItem);

    public abstract void process(JSONObject longRunningProcessQueueItem);
}
