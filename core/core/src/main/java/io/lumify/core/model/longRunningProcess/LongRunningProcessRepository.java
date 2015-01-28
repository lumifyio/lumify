package io.lumify.core.model.longRunningProcess;

import io.lumify.core.user.User;
import org.json.JSONObject;
import org.securegraph.Authorizations;

import java.util.List;

public abstract class LongRunningProcessRepository {
    public static final String VISIBILITY_STRING = "longRunningProcess";
    public static final String LONG_RUNNING_PROCESS_CONCEPT_IRI = "http://lumify.io/longRunningProcess#longRunningProcess";
    public static final String LONG_RUNNING_PROCESS_TO_USER_EDGE_IRI = "http://lumify.io/longRunningProcess#hasLongRunningProcess";
    public static final String LONG_RUNNING_PROCESS_ID_PREFIX = "LONG_RUNNING_PROCESS_";
    public static final String OWL_IRI = "http://lumify.io/longRunningProcess";

    public abstract String enqueue(JSONObject longRunningProcessQueueItem, User user, Authorizations authorizations);

    public void beginWork(JSONObject longRunningProcessQueueItem) {
    }

    public abstract void ack(JSONObject longRunningProcessQueueItem);

    public abstract void nak(JSONObject longRunningProcessQueueItem, Throwable ex);

    public abstract List<JSONObject> getLongRunningProcesses(User user);

    public abstract JSONObject findById(String longRunningProcessId, User user);

    public abstract void cancel(String longRunningProcessId, User user);

    public abstract void reportProgress(JSONObject longRunningProcessQueueItem, double progressPercent, String message);

    public abstract void delete(String longRunningProcessId, User authUser);
}
