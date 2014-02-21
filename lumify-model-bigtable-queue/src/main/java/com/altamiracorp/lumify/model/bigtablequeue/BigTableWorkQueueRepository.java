package com.altamiracorp.lumify.model.bigtablequeue;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.model.bigtablequeue.model.QueueItem;
import com.altamiracorp.lumify.model.bigtablequeue.model.QueueItemRepository;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BigTableWorkQueueRepository extends WorkQueueRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BigTableWorkQueueRepository.class);
    public static final String DEFAULT_TABLE_PREFIX = "atc_accumuloqueue_";
    private ModelSession modelSession;
    private UserProvider userProvider;
    private Map<String, QueueItemRepository> queues = new HashMap<String, QueueItemRepository>();
    private String tablePrefix;
    private User user;

    @Override
    public void init(Map config) {
        super.init(config);

        this.tablePrefix = getTablePrefix(config);
        if (this.tablePrefix == null) {
            this.tablePrefix = DEFAULT_TABLE_PREFIX;
        }
    }

    public static String getTablePrefix(Map config) {
        return (String) config.get(Configuration.WORK_QUEUE_REPOSITORY + ".tableprefix");
    }

    @Override
    public Object createSpout(Configuration configuration, String queueName, Long queueStartOffsetTime) {
        return new BigtableWorkQueueSpout(configuration, queueName);
    }

    @Override
    public void flush() {
        for (QueueItemRepository queue : queues.values()) {
            queue.flush();
        }
    }

    @Override
    public void format() {
        LOGGER.debug("BEGIN format");
        ModelUserContext ctx = this.userProvider.getSystemUser().getModelUserContext();
        List<String> tableList = this.modelSession.getTableList(ctx);
        for (String tableName : tableList) {
            if (tableName.startsWith(this.tablePrefix)) {
                LOGGER.info("Deleting queue table: " + tableName);
                this.modelSession.deleteTable(tableName, ctx);
            }
        }
        LOGGER.debug("END format");
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, String... extra) {
        String tableName = getTableName(this.tablePrefix, queueName);

        if (this.user == null) {
            this.user = this.userProvider.getSystemUser();
        }

        QueueItemRepository queue = this.queues.get(queueName);
        if (queue == null) {
            this.modelSession.initializeTable(tableName, this.user.getModelUserContext());
            queue = new QueueItemRepository(this.modelSession, tableName);
            this.queues.put(queueName, queue);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("push on queue %s: %s", tableName, json.toString(2));
        }
        QueueItem queueItem = createQueueItem(tableName, json, extra);
        queue.save(queueItem, flushFlag);
    }

    public static QueueItem createVertexIdQueueItem(String queueTableName, Object vertexId) {
        JSONObject json = contentToJson(ImmutableMap.<String, String>of(KEY_GRAPH_VERTEX_ID, vertexId.toString()));
        return createQueueItem(queueTableName, json);
    }

    public static QueueItem createQueueItem(String queueTableName, JSONObject json, String... extra) {
        return new QueueItem(queueTableName, json, extra);
    }

    public static String getTableName(String tablePrefix, String queueName) {
        return tablePrefix + queueName;
    }

    @Inject
    public void setModelSession(ModelSession modelSession) {
        this.modelSession = modelSession;
    }

    @Inject
    public void setUserProvider(UserProvider userProvider) {
        this.userProvider = userProvider;
    }
}
