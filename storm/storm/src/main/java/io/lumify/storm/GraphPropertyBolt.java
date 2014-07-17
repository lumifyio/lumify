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
import io.lumify.core.config.ConfigurationHelper;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.*;
import io.lumify.core.metrics.JmxMetricsManager;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ServiceLoaderUtil;
import io.lumify.core.util.TeeInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.property.StreamingPropertyValue;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.securegraph.util.IterableUtils.toList;

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
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(stormConf).createConfiguration();
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(configuration));

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
        Collection<GraphPropertyWorker> workers = InjectHelper.getInjectedServices(GraphPropertyWorker.class);
        this.workerWrappers = new ArrayList<GraphPropertyThreadedWrapper>(workers.size());
        for (GraphPropertyWorker worker : workers) {
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
            String hdfsRootDir = (String) stormConf.get(io.lumify.core.config.Configuration.HADOOP_URL);
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
        String propertyKey = json.optString("propertyKey");
        String propertyName = json.optString("propertyName");

        Object graphVertexId = json.opt("graphVertexId");
        if (graphVertexId != null) {
            Vertex vertex = graph.getVertex(graphVertexId, this.authorizations);
            if (vertex == null) {
                throw new LumifyException("Could not find vertex with id " + graphVertexId);
            }
            safeExecute(vertex, propertyKey, propertyName);
            return;
        }

        Object graphEdgeId = json.opt("graphEdgeId");
        if (graphEdgeId != null) {
            Edge edge = graph.getEdge(graphEdgeId, this.authorizations);
            if (edge == null) {
                throw new LumifyException("Could not find edge with id " + graphEdgeId);
            }
            safeExecute(edge, propertyKey, propertyName);
            return;
        }

        throw new LumifyException("Could not find graphVertexId or graphEdgeId");
    }

    private void safeExecute(Element element, String propertyKey, String propertyName) throws Exception {
        Property property;
        if ((propertyKey == null || propertyKey.length() == 0) && (propertyName == null || propertyName.length() == 0)) {
            property = null;
        } else {
            if (propertyKey == null) {
                property = element.getProperty(propertyName);
            } else {
                property = element.getProperty(propertyKey, propertyName);
            }
            if (property == null) {
                LOGGER.error("Could not find property [%s]:[%s] on vertex with id %s", propertyKey, propertyName, element.getId());
                return;
            }
        }
        safeExecute(element, property);
    }

    private void safeExecute(Element element, Property property) throws Exception {
        String propertyText = property == null ? "[none]" : (property.getKey() + ":" + property.getName());

        List<GraphPropertyThreadedWrapper> interestedWorkerWrappers = findInterestedWorkers(element, property);
        if (interestedWorkerWrappers.size() == 0) {
            LOGGER.info("Could not find interested workers for element %s property %s", element.getId(), propertyText);
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
                LOGGER.debug("interested worker for element %s property %s: %s", element.getId(), propertyText, interestedWorkerWrapper.getWorker().getClass().getName());
            }
        }
        GraphPropertyWorkData workData = new GraphPropertyWorkData(element, property);

        LOGGER.debug("Begin work on element %s property %s", element.getId(), propertyText);
        if (property != null && property.getValue() instanceof StreamingPropertyValue) {
            StreamingPropertyValue spb = (StreamingPropertyValue) property.getValue();
            safeExecuteStreamingPropertyValue(interestedWorkerWrappers, workData, spb);
        } else {
            safeExecuteNonStreamingProperty(interestedWorkerWrappers, workData);
        }

        this.graph.flush();

        LOGGER.debug("Completed work on %s", propertyText);
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
            for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
                interestedWorkerWrapper.dequeueResult();
            }
        } finally {
            if (tempFile != null) {
                if (!tempFile.delete()) {
                    LOGGER.warn("Could not delete temp file %s", tempFile.getAbsolutePath());
                }
            }
        }
    }

    private File copyToTempFile(InputStream in, GraphPropertyWorkData workData) throws IOException {
        String fileExt = RawLumifyProperties.FILE_NAME_EXTENSION.getPropertyValue(workData.getElement());
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

    private List<GraphPropertyThreadedWrapper> findInterestedWorkers(Element element, Property property) {
        List<GraphPropertyThreadedWrapper> interestedWorkers = new ArrayList<GraphPropertyThreadedWrapper>();
        for (GraphPropertyThreadedWrapper wrapper : workerWrappers) {
            if (wrapper.getWorker().isHandled(element, property)) {
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
