/*
 * Copyright 2014 Altamira Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.altamiracorp.lumify.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.metrics.JmxMetricsManager;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A Storm bolt that collects and reports metrics on the
 * Tuples it processes for the Lumify system.
 */
public abstract class BaseLumifyMetricsBolt extends BaseRichBolt {
    /**
     * The class logger.
     */
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseLumifyMetricsBolt.class);
    
    /**
     * The total processed metric suffix.
     */
    private static final String TOTAL_PROCESSED_METRIC = "total-processed";
    
    /**
     * The processing metric suffix.
     */
    private static final String PROCESSING_METRIC = "processing";
    
    /**
     * The total errors metric suffix.
     */
    private static final String TOTAL_ERRORS_METRIC = "total-errors";
    
    /**
     * The processing time metric suffix.
     */
    private static final String PROCESSING_TIME_METRIC = "processing-time";
    
    /**
     * The bolt ID as it is configured in the Storm topology: ${componentId}-${taskId}
     */
    private String boltId;
    
    /**
     * The configured OutputCollector.
     */
    private OutputCollector outputCollector;
    
    /**
     * The metrics manager.
     */
    private JmxMetricsManager metricsManager;
    
    /**
     * This counter indicates the total number of Tuples that
     * have been fully processed by this bolt.
     */
    private Counter totalProcessedCounter;
    
    /**
     * This counter indicates the number of Tuples that are
     * currently being processed by this bolt.
     */
    private Counter processingCounter;
    
    /**
     * This counter indicates the number of Tuples that
     * encountered an error during processing by this bolt.
     */
    private Counter totalErrorCounter;
    
    /**
     * This timer indicates the total amount of processing time
     * spent by this bolt.
     */
    private Timer processingTimeTimer;
    
    @Override
    public final void prepare(final Map stormConf, final TopologyContext context, final OutputCollector collector) {
        boltId = String.format("%s-%d", context.getThisComponentId(), context.getThisTaskId());
        LOGGER.info("Initializing bolt: %s", boltId);
        this.outputCollector = collector;
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(new Configuration(stormConf)));
        
        String namePrefix = metricsManager.getNamePrefix(this);
        MetricRegistry registry = metricsManager.getRegistry();
        totalProcessedCounter = registry.counter(String.format("%s%s", namePrefix, TOTAL_PROCESSED_METRIC));
        processingCounter = registry.counter(String.format("%s%s", namePrefix, PROCESSING_METRIC));
        totalErrorCounter = registry.counter(String.format("%s%s", namePrefix, TOTAL_ERRORS_METRIC));
        processingTimeTimer = registry.timer(String.format("%s%s", namePrefix, PROCESSING_TIME_METRIC));
        
        doPrepare(stormConf, context, collector);
    }
    
    /**
     * This method is called after the BaseLumifyMetricsBolt completes its initialization
     * as the topology is created.  Subclasses may override it to perform additional initialization.
     * @param stormConf the Storm configuration
     * @param context the Topology context
     * @param collector the output collector
     */
    protected void doPrepare(final Map stormConf, final TopologyContext context, final OutputCollector collector) {
    }
    
    @Override
    public final void execute(final Tuple input) {
        String processId = getProcessId();
        processingCounter.inc();
        Timer.Context procTimeCtx = processingTimeTimer.time();
        try {
            LOGGER.info("[Bolt: %s::%s] BEGIN Message [%s]", processId, boltId, input.getMessageId());
            LOGGER.debug("[Bolt: %s::%s] Message [%s] Payload: %s", processId, boltId, input.getMessageId(), input);
            safeExecute(input);
            LOGGER.info("[Bolt: %s::%s] ACK Message [%s]", processId, boltId, input.getMessageId());
            outputCollector.ack(input);
        } catch (Exception ex) {
            totalErrorCounter.inc();
            LOGGER.warn("[Bolt: %s::%s] FAIL Message [%s]", processId, boltId, input.getMessageId(), ex);
            outputCollector.reportError(ex);
            outputCollector.fail(input);
        } finally {
            processingCounter.dec();
            totalProcessedCounter.inc();
            procTimeCtx.stop();
            LOGGER.info("[Bolt: %s::%s] END Message [%s]", processId, boltId, input.getMessageId());
        }
    }
    
    /**
     * Subclasses should implement their execution logic in this method, throwing
     * an exception in the event of a processing error.
     * @param input the input Tuple
     * @throws Exception if an error occurs during processing
     */
    protected abstract void safeExecute(final Tuple input) throws Exception;

    /**
     * Emit a new Tuple containing the provided values using the
     * input Tuple as the anchor to the default stream.
     * @param anchor the parent Tuple the new Tuple will be anchored to
     * @param tuple the values for the emitted Tuple
     * @return the list of Task IDs the new Tuple was sent to
     */
    protected final List<Integer> emit(Tuple anchor, Object... tuple) {
        List<Object> tupleList = tuple.length > 0 ? Arrays.asList(tuple) : Collections.EMPTY_LIST;
        return outputCollector.emit(anchor, tupleList);
    }

    /**
     * Emit a new Tuple containing the provided values using the
     * input Tuple as the anchor to the default stream.
     * @param anchor the parent Tuple the new Tuple will be anchored to
     * @param tuple the values for the emitted Tuple
     * @return the list of Task IDs the new Tuple was sent to
     */
    protected final List<Integer> emit(Tuple anchor, List<Object> tuple) {
        return outputCollector.emit(anchor, tuple);
    }
    
    /**
     * Returns the fully qualified class name as the default process ID.  Subclasses
     * may choose to override this method to change the process ID.
     * @return the process ID
     */
    protected String getProcessId() {
        return getClass().getName();
    }
    
    /**
     * Gets the Bolt ID as configured in the current Storm topology: ${componentId}-${taskId}
     * @return the bolt ID in the current Storm topology
     */
    protected final String getBoltId() {
        return boltId;
    }
    
    @Inject
    public final void setMetricsManager(final JmxMetricsManager manager) {
        metricsManager = manager;
    }
}
