package com.altamiracorp.lumify.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.ConfigurationHelper;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.ingest.graphProperty.*;
import com.altamiracorp.lumify.core.metrics.JmxMetricsManager;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.ServiceLoaderUtil;
import com.altamiracorp.lumify.core.util.TeeInputStream;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;

public class GraphPropertyBolt extends BaseRichBolt {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphPropertyBolt.class);

    public static final String JSON_OUTPUT_FIELD = "json";

    private Graph graph;
    private OutputCollector collector;
    private User user;
    private UserRepository userRepository;
    private Authorizations authorizations;

    private JmxMetricsManager metricsManager;
    private Counter totalProcessedCounter;
    private Counter processingCounter;
    private Counter totalErrorCounter;
    private Timer processingTimeTimer;
    private List<GraphPropertyThreadedWrapper> workerWrappers;

    @Override
    public void prepare(final Map stormConf, TopologyContext context, OutputCollector collector) {
        LOGGER.info("Configuring environment for bolt: %s-%d", context.getThisComponentId(), context.getThisTaskId());
        this.collector = collector;
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(new com.altamiracorp.lumify.core.config.Configuration(stormConf)));

        prepareJmx();
        prepareUser(stormConf);
        prepareWorkers(stormConf);
    }

    private void prepareWorkers(Map stormConf) {
        FileSystem hdfsFileSystem = getFileSystem(stormConf);

        List<TermMentionFilter> termMentionFilters = loadTermMentionFilters(stormConf, hdfsFileSystem);

        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(
                stormConf,
                termMentionFilters,
                hdfsFileSystem,
                this.user,
                this.authorizations,
                InjectHelper.getInjector());
        List<GraphPropertyWorker> workers = toList(ServiceLoaderUtil.load(GraphPropertyWorker.class));
        this.workerWrappers = new ArrayList<GraphPropertyThreadedWrapper>(workers.size());
        for (GraphPropertyWorker worker : workers) {
            InjectHelper.inject(worker);
            try {
                worker.prepare(workerPrepareData);
            } catch (Exception ex) {
                throw new LumifyException("Could not prepare graph property worker " + worker.getClass().getName(), ex);
            }

            GraphPropertyThreadedWrapper wrapper = new GraphPropertyThreadedWrapper(worker);
            InjectHelper.inject(wrapper);
            workerWrappers.add(wrapper);
            Thread thread = new Thread(wrapper);
            String workerName = worker.getClass().getName();
            thread.setName("graphPropertyWorker-" + workerName);
            thread.start();
        }
    }

    private FileSystem getFileSystem(Map stormConf) {
        FileSystem hdfsFileSystem;
        Configuration conf = ConfigurationHelper.createHadoopConfigurationFromMap(stormConf);
        try {
            String hdfsRootDir = (String) stormConf.get(com.altamiracorp.lumify.core.config.Configuration.HADOOP_URL);
            hdfsFileSystem = FileSystem.get(new URI(hdfsRootDir), conf, "hadoop");
        } catch (Exception e) {
            throw new LumifyException("Could not open hdfs filesystem", e);
        }
        return hdfsFileSystem;
    }

    private List<TermMentionFilter> loadTermMentionFilters(Map stormConf, FileSystem hdfsFileSystem) {
        TermMentionFilterPrepareData termMentionFilterPrepareData = new TermMentionFilterPrepareData(
                stormConf,
                hdfsFileSystem,
                this.user,
                this.authorizations,
                InjectHelper.getInjector()
        );

        List<TermMentionFilter> termMentionFilters = toList(ServiceLoaderUtil.load(TermMentionFilter.class));
        for (TermMentionFilter termMentionFilter : termMentionFilters) {
            InjectHelper.inject(termMentionFilter);
            try {
                termMentionFilter.prepare(termMentionFilterPrepareData);
            } catch (Exception ex) {
                throw new LumifyException("Could not initialize term mention filter: " + termMentionFilter.getClass().getName(), ex);
            }
        }
        return termMentionFilters;
    }

    private void prepareUser(Map stormConf) {
        this.user = (User) stormConf.get("user");
        if (this.user == null) {
            this.user = this.userRepository.getSystemUser();
        }
        this.authorizations = this.userRepository.getAuthorizations(this.user);
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
            LOGGER.debug("BEGIN %s: [MessageID: %s]", getClass().getName(), input.getMessageId());
            LOGGER.trace("BEGIN %s: [MessageID: %s] %s", getClass().getName(), input.getMessageId(), input);
            try {
                safeExecute(input);
                LOGGER.debug("ACK'ing: [MessageID: %s]", input.getMessageId());
                LOGGER.trace("ACK'ing: [MessageID: %s] %s", input.getMessageId(), input);
                this.collector.ack(input);
            } catch (Exception e) {
                totalErrorCounter.inc();
                LOGGER.error("Error occurred during execution: " + input, e);
                this.collector.reportError(e);
                this.collector.fail(input);
            }

            LOGGER.debug("END %s: [MessageID: %s]", getClass().getName(), input.getMessageId());
            LOGGER.trace("END %s: [MessageID: %s] %s", getClass().getName(), input.getMessageId(), input);
        } finally {
            processingCounter.dec();
            totalProcessedCounter.inc();
            processingTimeContext.stop();
        }
    }

    private void safeExecute(Tuple input) throws Exception {
        JSONObject json = getJsonFromTuple(input);
        Object graphVertexId = json.get("graphVertexId");
        String propertyKey = json.optString("propertyKey");
        String propertyName = json.getString("propertyName");
        safeExecute(graphVertexId, propertyKey, propertyName);
    }

    private void safeExecute(Object graphVertexId, String propertyKey, String propertyName) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, this.authorizations);
        if (vertex == null) {
            throw new LumifyException("Could not find vertex with id " + graphVertexId);
        }
        Property property;
        if (propertyKey == null) {
            property = vertex.getProperty(propertyName);
        } else {
            property = vertex.getProperty(propertyKey, propertyName);
        }
        if (property == null) {
            LOGGER.error("Could not find property [%s]:[%s] on vertex with id %s", propertyKey, propertyName, graphVertexId);
            return;
        }
        safeExecute(vertex, property);
    }

    private void safeExecute(Vertex vertex, Property property) throws Exception {
        List<GraphPropertyThreadedWrapper> interestedWorkerWrappers = findInterestedWorkers(vertex, property);
        if (interestedWorkerWrappers.size() == 0) {
            LOGGER.info("Could not find interested workers for property %s:%s", property.getKey(), property.getName());
        }
        GraphPropertyWorkData workData = new GraphPropertyWorkData(vertex, property);

        LOGGER.debug("Begin work on %s:%s", property.getKey(), property.getName());
        if (property.getValue() instanceof StreamingPropertyValue) {
            StreamingPropertyValue spb = (StreamingPropertyValue) property.getValue();
            safeExecuteStreamingPropertyValue(interestedWorkerWrappers, workData, spb);
        } else {
            safeExecuteNonStreamingProperty(interestedWorkerWrappers, workData);
        }

        this.graph.flush();

        LOGGER.debug("Completed work on %s:%s", property.getKey(), property.getName());
    }

    private void safeExecuteNonStreamingProperty(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers, GraphPropertyWorkData workData) throws Exception {
        for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
            interestedWorkerWrapper.getWorker().execute(null, workData);
        }
    }

    private void safeExecuteStreamingPropertyValue(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers, GraphPropertyWorkData workData, StreamingPropertyValue streamingPropertyValue) throws Exception {
        String[] workerNames = graphPropertyThreadedWrapperToNames(interestedWorkerWrappers);
        InputStream in = streamingPropertyValue.getInputStream();
        File tempFile = null;
        try {
            boolean requiresLocalFile = isLocalFileRequired(interestedWorkerWrappers);
            if (requiresLocalFile) {
                tempFile = copyToTempFile(in, workData);
                in = new FileInputStream(tempFile);
            }

            TeeInputStream teeInputStream = new TeeInputStream(in, workerNames);
            for (int i = 0; i < interestedWorkerWrappers.size(); i++) {
                interestedWorkerWrappers.get(i).enqueueWork(teeInputStream.getTees()[i], workData);
            }
            teeInputStream.loopUntilTeesAreClosed();
        } finally {
            if (tempFile != null) {
                if (!tempFile.delete()) {
                    LOGGER.warn("Could not delete temp file %s", tempFile.getAbsolutePath());
                }
            }
        }
    }

    private File copyToTempFile(InputStream in, GraphPropertyWorkData workData) throws IOException {
        String fileExt = RawLumifyProperties.FILE_NAME_EXTENSION.getPropertyValue(workData.getVertex());
        if (fileExt == null) {
            fileExt = "data";
        }
        File tempFile = File.createTempFile("graphPropertyBolt", fileExt);
        workData.setLocalFile(tempFile);
        OutputStream tempFileOut = new FileOutputStream(tempFile);
        try {
            IOUtils.copy(in, tempFileOut);
        } finally {
            in.close();
            tempFileOut.close();
        }
        return tempFile;
    }

    private boolean isLocalFileRequired(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers) {
        for (GraphPropertyThreadedWrapper worker : interestedWorkerWrappers) {
            if (worker.getWorker().isLocalFileRequired()) {
                return true;
            }
        }
        return false;
    }

    private String[] graphPropertyThreadedWrapperToNames(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers) {
        String[] names = new String[interestedWorkerWrappers.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = interestedWorkerWrappers.get(i).getWorker().getClass().getName();
        }
        return names;
    }

    private List<GraphPropertyThreadedWrapper> findInterestedWorkers(Vertex vertex, Property property) {
        List<GraphPropertyThreadedWrapper> interestedWorkers = new ArrayList<GraphPropertyThreadedWrapper>();
        for (GraphPropertyThreadedWrapper wrapper : workerWrappers) {
            if (wrapper.getWorker().isHandled(vertex, property)) {
                interestedWorkers.add(wrapper);
            }
        }
        return interestedWorkers;
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
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setMetricsManager(JmxMetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}
