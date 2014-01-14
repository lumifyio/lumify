package com.altamiracorp.lumify.model.bigtablequeue;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.metrics.MetricsManager;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.model.bigtablequeue.model.QueueItem;
import com.altamiracorp.lumify.model.bigtablequeue.model.QueueItemRepository;
import com.altamiracorp.lumify.model.bigtablequeue.model.QueueItemRowKey;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.google.inject.Inject;
import com.google.inject.Module;

import java.util.HashMap;
import java.util.Map;

public class BigtableWorkQueueSpout extends BaseRichSpout {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BigtableWorkQueueSpout.class);
    private final String queueName;
    private final String tablePrefix;
    private ModelSession modelSession;
    private String tableName;
    private User user;
    private QueueItemRepository queueItemRepository;
    private SpoutOutputCollector collector;
    private Map<String, Boolean> workingSet = new HashMap<String, Boolean>();
    private MetricsManager metricsManager;
    private Counter totalProcessedCounter;
    private Counter totalErrorCounter;

    public BigtableWorkQueueSpout(Configuration configuration, String queueName) {
        this.queueName = queueName;
        this.tablePrefix = configuration.get(Configuration.WORK_QUEUE_REPOSITORY + ".tableprefix", BigTableWorkQueueRepository.DEFAULT_TABLE_PREFIX);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("json"));
    }

    @Override
    public void open(final Map conf, TopologyContext topologyContext, SpoutOutputCollector collector) {
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(new Configuration(conf)));

        this.collector = collector;
        this.user = new SystemUser();
        this.tableName = BigTableWorkQueueRepository.getTableName(this.tablePrefix, this.queueName);
        this.modelSession.initializeTable(this.tableName, user.getModelUserContext());
        this.queueItemRepository = new QueueItemRepository(this.modelSession, this.tableName);

        String namePrefix = metricsManager.getNamePrefix(this, this.queueName);
        registerMetrics(metricsManager, namePrefix);
    }

    private void registerMetrics(MetricsManager metricsManager, String namePrefix) {
        totalProcessedCounter = metricsManager.getRegistry().counter(namePrefix + "total-processed");
        totalErrorCounter = metricsManager.getRegistry().counter(namePrefix + "total-errors");
        metricsManager.getRegistry().register(namePrefix + "in-process",
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return workingSet.size();
                    }
                });
    }

    @Override
    public void nextTuple() {
        try {
            Iterable<Row> rows = this.modelSession.findAll(this.tableName, this.user.getModelUserContext());
            for (Row row : rows) {
                String rowKeyString = row.getRowKey().toString();
                if (this.workingSet.containsKey(rowKeyString)) {
                    continue;
                }
                QueueItem queueItem = this.queueItemRepository.fromRow(row);
                this.workingSet.put(rowKeyString, true);
                this.collector.emit(new Values(queueItem.getJson().toString()), rowKeyString);
                return;
            }
            Utils.sleep(1000);
        } catch (Exception ex) {
            LOGGER.error("Could not get next tuple (" + this.tableName + ")", ex);
            this.collector.reportError(ex);
            Utils.sleep(10000);
        }
    }

    @Override
    public void ack(Object msgId) {
        try {
            LOGGER.debug("ack (%s): %s", this.tableName, msgId.toString());
            totalProcessedCounter.inc();
            QueueItemRowKey rowKey = new QueueItemRowKey(msgId);
            this.queueItemRepository.delete(rowKey, this.user.getModelUserContext());
            this.workingSet.remove(rowKey.toString());
            super.ack(msgId);
        } catch (Exception ex) {
            LOGGER.error("Could not ack (" + this.tableName + "): " + msgId, ex);
            this.collector.reportError(ex);
        }
    }

    @Override
    public void fail(Object msgId) {
        try {
            LOGGER.debug("fail (%s): %s", this.tableName, msgId.toString());
            totalErrorCounter.inc();
            super.fail(msgId);
        } catch (Exception ex) {
            LOGGER.error("Could not fail (" + this.tableName + "): " + msgId, ex);
            this.collector.reportError(ex);
        }
    }

    @Inject
    public void setModelSession(ModelSession modelSession) {
        this.modelSession = modelSession;
    }

    @Inject
    public void setMetricsManager(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }
}
