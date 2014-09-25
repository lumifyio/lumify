package io.lumify.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyRunner;
import io.lumify.core.metrics.JmxMetricsManager;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

import java.util.Map;

public class GraphPropertyBolt extends BaseRichBolt {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphPropertyBolt.class);

    public static final String JSON_OUTPUT_FIELD = "json";

    private OutputCollector collector;

    private JmxMetricsManager metricsManager;
    private Counter totalProcessedCounter;
    private Counter processingCounter;
    private Counter totalErrorCounter;
    private Timer processingTimeTimer;
    private GraphPropertyRunner graphPropertyRunner;

    @Override
    public void prepare(final Map stormConf, TopologyContext context, OutputCollector collector) {
        LOGGER.info("Configuring environment for bolt: %s-%d", context.getThisComponentId(), context.getThisTaskId());
        this.collector = collector;
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(stormConf).createConfiguration();
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(configuration));

        prepareJmx();
        graphPropertyRunner.prepare(stormConf);
    }

    private void prepareJmx() {
        String namePrefix = metricsManager.getNamePrefix(this);
        totalProcessedCounter = metricsManager.getRegistry().counter(namePrefix + "total-processed");
        processingCounter = metricsManager.getRegistry().counter(namePrefix + "processing");
        totalErrorCounter = metricsManager.getRegistry().counter(namePrefix + "total-errors");
        processingTimeTimer = metricsManager.getRegistry().timer(namePrefix + "processing-time");
    }

    @Override
    public void execute(Tuple input) {
        processingCounter.inc();
        Timer.Context processingTimeContext = processingTimeTimer.time();
        try {
            LOGGER.debug("BEGIN %s: [MessageID: %s] %s", getClass().getName(), input.getMessageId(), input);
            try {
                safeExecute(input);
                LOGGER.debug("ACK'ing: [MessageID: %s] %s", input.getMessageId(), input);
                this.collector.ack(input);
            } catch (Exception e) {
                totalErrorCounter.inc();
                LOGGER.error("Error occurred during execution: " + input, e);
                this.collector.reportError(e);
                this.collector.fail(input);
            }

            LOGGER.debug("END %s: [MessageID: %s] %s", getClass().getName(), input.getMessageId(), input);
        } finally {
            processingCounter.dec();
            totalProcessedCounter.inc();
            processingTimeContext.stop();
        }
    }

    private void safeExecute(Tuple input) throws Exception {
        JSONObject json = getJsonFromTuple(input);
        graphPropertyRunner.process(json);
    }

    protected JSONObject getJsonFromTuple(Tuple input) throws Exception {
        String str = input.getString(0);
        try {
            return new JSONObject(str);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid input format. Expected JSON got.\n" + str, ex);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(JSON_OUTPUT_FIELD));
    }

    @Inject
    public void setGraphPropertyRunner(GraphPropertyRunner graphPropertyRunner) {
        this.graphPropertyRunner = graphPropertyRunner;
    }

    @Inject
    public void setMetricsManager(JmxMetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }
}
