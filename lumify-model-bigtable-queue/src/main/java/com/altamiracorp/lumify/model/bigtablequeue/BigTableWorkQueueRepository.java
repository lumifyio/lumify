package com.altamiracorp.lumify.model.bigtablequeue;

import backtype.storm.topology.IRichSpout;
import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.model.bigtablequeue.model.QueueItemRepository;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BigTableWorkQueueRepository extends WorkQueueRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BigTableWorkQueueRepository.class);
    public static final String DEFAULT_TABLE_PREFIX = "atc_accumuloqueue_";
    private ModelSession modelSession;
    private Map<String, Boolean> queues = new HashMap<String, Boolean>();
    private String tablePrefix;
    private User user;
    private QueueItemRepository queueItemRepository;

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
        this.queueItemRepository.flush();
    }

    @Override
    public void format() {
        ModelUserContext ctx = new SystemUser().getModelUserContext();
        List<String> tableList = this.modelSession.getTableList(ctx);
        for (String tableName : tableList) {
            if (tableName.startsWith(this.tablePrefix)) {
                LOGGER.info("Deleting queue table: " + tableName);
                this.modelSession.deleteTable(tableName, ctx);
            }
        }
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, String... extra) {
        String tableName = getTableName(this.tablePrefix, queueName);

        if (this.user == null) {
            this.user = new SystemUser();
        }
        if (this.queueItemRepository == null) {
            this.queueItemRepository = new QueueItemRepository(this.modelSession, tableName);
        }

        if (!this.queues.containsKey(queueName)) {
            this.modelSession.initializeTable(tableName, this.user.getModelUserContext());
            this.queues.put(queueName, true);
        }

        this.queueItemRepository.add(json, extra, flushFlag, user);
    }

    static String getTableName(String tablePrefix, String queueName) {
        return tablePrefix + queueName;
    }

    @Inject
    public void setModelSession(ModelSession modelSession) {
        this.modelSession = modelSession;
    }
}
