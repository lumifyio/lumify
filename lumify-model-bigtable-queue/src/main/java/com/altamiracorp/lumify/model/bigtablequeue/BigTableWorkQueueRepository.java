package com.altamiracorp.lumify.model.bigtablequeue;

import backtype.storm.topology.IRichSpout;
import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.model.bigtablequeue.model.QueueItemRepository;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BigTableWorkQueueRepository extends WorkQueueRepository {
    public static final String DEFAULT_TABLE_PREFIX = "atc_accumuloqueue_";
    private ModelSession modelSession;
    private Map<String, QueueItemRepository> queues = new HashMap<String, QueueItemRepository>();
    private String tablePrefix;
    private User user;

    @Override
    public void init(Map config) {
        super.init(config);

        this.tablePrefix = (String) config.get(Configuration.WORK_QUEUE_REPOSITORY + ".tableprefix");
        if (this.tablePrefix == null) {
            this.tablePrefix = DEFAULT_TABLE_PREFIX;
        }
    }

    @Override
    public IRichSpout createSpout(Configuration configuration, String queueName, Long queueStartOffsetTime) {
        return new BigtableWorkQueueSpout(configuration, queueName);
    }

    @Override
    public void flush() {
        for (QueueItemRepository queue : queues.values()) {
            queue.flush();
        }
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, String... extra) {
        String tableName = getTableName(this.tablePrefix, queueName);

        if (this.user == null) {
            this.user = new SystemUser();
        }

        QueueItemRepository queue = this.queues.get(queueName);
        if (queue == null) {
            this.modelSession.initializeTable(tableName, this.user.getModelUserContext());
            queue = new QueueItemRepository(this.modelSession, tableName);
            this.queues.put(queueName, queue);
        }

        LOGGER.debug("push on queue %s", tableName);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("push on queue %s: %s", tableName, json.toString(2));
        }
        queue.add(json, extra, flushFlag, user);
    }

    static String getTableName(String tablePrefix, String queueName) {
        return tablePrefix + queueName;
    }

    @Inject
    public void setModelSession(ModelSession modelSession) {
        this.modelSession = modelSession;
    }
}
