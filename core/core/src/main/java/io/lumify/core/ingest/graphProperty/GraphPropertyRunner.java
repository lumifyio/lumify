package io.lumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.NoOpWorkQueueRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ServiceLoaderUtil;
import io.lumify.core.util.TeeInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.property.StreamingPropertyValue;
import org.securegraph.util.IterableUtils;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.securegraph.util.IterableUtils.toList;

public class GraphPropertyRunner {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphPropertyRunner.class);
    private Graph graph;
    private Authorizations authorizations;
    private List<GraphPropertyThreadedWrapper> workerWrappers;
    private User user;
    private UserRepository userRepository;
    private Configuration configuration;
    private WorkQueueRepository workQueueRepository;

    public void prepare(User user) {
        this.user = user;
        this.authorizations = this.userRepository.getAuthorizations(this.user);
        prepareWorkers();
    }

    private void prepareWorkers() {
        FileSystem hdfsFileSystem = getFileSystem();

        List<TermMentionFilter> termMentionFilters = loadTermMentionFilters(hdfsFileSystem);

        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(
                configuration.toMap(),
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

    private FileSystem getFileSystem() {
        FileSystem hdfsFileSystem;
        org.apache.hadoop.conf.Configuration conf = configuration.toHadoopConfiguration();
        try {
            String hdfsRootDir = configuration.get(Configuration.HADOOP_URL, null);
            hdfsFileSystem = FileSystem.get(new URI(hdfsRootDir), conf, "hadoop");
        } catch (Exception e) {
            throw new LumifyException("Could not open hdfs filesystem", e);
        }
        return hdfsFileSystem;
    }

    private List<TermMentionFilter> loadTermMentionFilters(FileSystem hdfsFileSystem) {
        TermMentionFilterPrepareData termMentionFilterPrepareData = new TermMentionFilterPrepareData(
                configuration.toMap(),
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

    public void process(JSONObject json) throws Exception {
        String propertyKey = json.optString("propertyKey");
        String propertyName = json.optString("propertyName");
        String workspaceId = json.optString("workspaceId");
        String visibilitySource = json.optString("visibilitySource");

        String graphVertexId = json.optString("graphVertexId");
        if (graphVertexId != null && graphVertexId.length() > 0) {
            Vertex vertex = graph.getVertex(graphVertexId, this.authorizations);
            if (vertex == null) {
                throw new LumifyException("Could not find vertex with id " + graphVertexId);
            }
            safeExecute(vertex, propertyKey, propertyName, workspaceId, visibilitySource);
            return;
        }

        String graphEdgeId = json.optString("graphEdgeId");
        if (graphEdgeId != null && graphEdgeId.length() > 0) {
            Edge edge = graph.getEdge(graphEdgeId, this.authorizations);
            if (edge == null) {
                throw new LumifyException("Could not find edge with id " + graphEdgeId);
            }
            safeExecute(edge, propertyKey, propertyName, workspaceId, visibilitySource);
            return;
        }

        throw new LumifyException("Could not find graphVertexId or graphEdgeId");
    }

    private void safeExecute(Element element, String propertyKey, String propertyName, String workspaceId, String visibilitySource) throws Exception {
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
        safeExecute(element, property, workspaceId, visibilitySource);
    }

    private void safeExecute(Element element, Property property, String workspaceId, String visibilitySource) throws Exception {
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

        GraphPropertyWorkData workData = new GraphPropertyWorkData(element, property, workspaceId, visibilitySource);

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
        String fileExt = LumifyProperties.FILE_NAME_EXTENSION.getPropertyValue(workData.getElement());
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

    private List<GraphPropertyThreadedWrapper> findInterestedWorkers(Element element, Property property) {
        Set<String> graphPropertyWorkerWhiteList = IterableUtils.toSet(LumifyProperties.GRAPH_PROPERTY_WORKER_WHITE_LIST.getPropertyValues(element));
        Set<String> graphPropertyWorkerBlackList = IterableUtils.toSet(LumifyProperties.GRAPH_PROPERTY_WORKER_BLACK_LIST.getPropertyValues(element));

        List<GraphPropertyThreadedWrapper> interestedWorkers = new ArrayList<GraphPropertyThreadedWrapper>();
        for (GraphPropertyThreadedWrapper wrapper : workerWrappers) {
            String graphPropertyWorkerName = wrapper.getWorker().getClass().getName();
            if (graphPropertyWorkerWhiteList.size() > 0 && !graphPropertyWorkerWhiteList.contains(graphPropertyWorkerName)) {
                continue;
            }
            if (graphPropertyWorkerBlackList.contains(graphPropertyWorkerName)) {
                continue;
            }
            if (wrapper.getWorker().isHandled(element, property)) {
                interestedWorkers.add(wrapper);
            }
        }
        return interestedWorkers;
    }

    private String[] graphPropertyThreadedWrapperToNames(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers) {
        String[] names = new String[interestedWorkerWrappers.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = interestedWorkerWrappers.get(i).getWorker().getClass().getName();
        }
        return names;
    }

    public void shutdown() {
        for (GraphPropertyThreadedWrapper wrapper : this.workerWrappers) {
            wrapper.stop();
        }
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    public void run() throws Exception {
        GraphPropertyWorkerSpout graphPropertyWorkerSpout = prepareGraphPropertyWorkerSpout();
        while (true) {
            GraphPropertyWorkerTuple tuple = graphPropertyWorkerSpout.nextTuple();
            if (tuple == null) {
                Thread.sleep(100);
                continue;
            }
            try {
                process(tuple.getJson());
                graphPropertyWorkerSpout.ack(tuple.getMessageId());
            } catch (Throwable ex) {
                LOGGER.error("Could not process tuple: %s", tuple, ex);
                graphPropertyWorkerSpout.fail(tuple.getMessageId());
            }
        }
    }

    protected GraphPropertyWorkerSpout prepareGraphPropertyWorkerSpout() {
        GraphPropertyWorkerSpout spout = workQueueRepository.createGraphPropertyWorkerSpout();
        spout.open();
        return spout;
    }
}
