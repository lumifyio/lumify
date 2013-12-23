package com.altamiracorp.lumify.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.config.ConfigurationHelper;
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.metrics.MetricsManager;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.codehaus.plexus.util.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class BaseLumifyBolt extends BaseRichBolt {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseLumifyBolt.class);
    private static final SimpleDateFormat fileNameSuffix = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ssZ");
    public static final String JSON_OUTPUT_FIELD = "json";

    /**
     * The file extensions that are considered archive files:
     * - .zip
     * - .tar
     */
    private static final Set<String> ARCHIVE_EXTENSIONS;

    static {
        Set<String> arcExts = new HashSet<String>();
        arcExts.add("zip");
        arcExts.add("tar");
        ARCHIVE_EXTENSIONS = Collections.unmodifiableSet(arcExts);
    }

    private OutputCollector collector;
    protected ArtifactRepository artifactRepository;
    protected OntologyRepository ontologyRepository;
    private FileSystem hdfsFileSystem;
    protected GraphRepository graphRepository;
    protected AuditRepository auditRepository;
    protected TermMentionRepository termMentionRepository;
    private Injector injector;
    private Counter totalProcessedCounter;
    private Counter processingCounter;
    private Counter totalErrorCounter;
    private Timer processingTimeTimer;
    private MetricsManager metricsManager;
    protected WorkQueueRepository workQueueRepository;
    private User user;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        LOGGER.info(String.format("Configuring environment for bolt: %s-%d", context.getThisComponentId(), context.getThisTaskId()));
        this.collector = collector;
        injector = Guice.createInjector(StormBootstrap.create(stormConf));
        injector.injectMembers(this);

        String namePrefix = metricsManager.getNamePrefix(this);
        totalProcessedCounter = metricsManager.getRegistry().counter(namePrefix + "total-processed");
        processingCounter = metricsManager.getRegistry().counter(namePrefix + "processing");
        totalErrorCounter = metricsManager.getRegistry().counter(namePrefix + "total-errors");
        processingTimeTimer = metricsManager.getRegistry().timer(namePrefix + "processing-time");

        user = (User) stormConf.get("user");
        if (user == null) {
            user = new SystemUser();
        }

        Configuration conf = ConfigurationHelper.createHadoopConfigurationFromMap(stormConf);
        try {
            String hdfsRootDir = (String) stormConf.get(com.altamiracorp.lumify.core.config.Configuration.HADOOP_URL);
            hdfsFileSystem = FileSystem.get(new URI(hdfsRootDir), conf, "hadoop");
        } catch (Exception e) {
            collector.reportError(e);
        }
    }

    protected JSONObject getJsonFromTuple(Tuple input) throws Exception {
        String str = input.getString(0);
        try {
            return new JSONObject(str);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid input format. Expected JSON got.\n" + str, ex);
        }
    }

    protected JSONObject tryGetJsonFromTuple(Tuple input) {
        try {
            return getJsonFromTuple(input);
        } catch (Exception ex) {
            return null;
        }
    }

    public Injector getInjector() {
        return injector;
    }

    protected <T> T inject(T obj) {
        getInjector().injectMembers(obj);
        return obj;
    }

    protected FileSystem getHdfsFileSystem() {
        return hdfsFileSystem;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(JSON_OUTPUT_FIELD));
    }

    @Override
    public final void execute(Tuple input) {
        processingCounter.inc();
        Timer.Context processingTimeContext = processingTimeTimer.time();
        try {
            String auditMessage;
            String graphVertexId = null;
            auditMessage = "BEGIN " + this.getClass().getName() + ": " + input;
            LOGGER.info(auditMessage);
            JSONObject json = tryGetJsonFromTuple(input);
            if (json != null) {
                graphVertexId = json.optString("graphVertexId");
                if (graphVertexId.length() == 0) {
                    graphVertexId = null;
                }
                if (graphVertexId != null) {
                    auditRepository.audit(graphVertexId, auditMessage, getUser());
                }
            }
            try {
                safeExecute(input);

                LOGGER.debug("ack'ing: " + input);
                getCollector().ack(input);
            } catch (Exception e) {
                totalErrorCounter.inc();
                LOGGER.error("Error occurred during execution: " + input, e);
                getCollector().reportError(e);
                getCollector().fail(input);
            }

            auditMessage = "END " + this.getClass().getName() + ": " + input;
            LOGGER.info(auditMessage);
            if (graphVertexId != null) {
                auditRepository.audit(graphVertexId, auditMessage, getUser());
            }
        } finally {
            processingCounter.dec();
            totalProcessedCounter.inc();
            processingTimeContext.stop();
        }
    }

    protected abstract void safeExecute(Tuple input) throws Exception;

    protected InputStream openFile(String fileName) throws Exception {
        // TODO probably a better way to handle this
        try {
            return new FileInputStream(fileName);
        } catch (Exception ex) {
            return getHdfsFileSystem().open(new Path(fileName));
        }
    }

    protected long getFileSize(String fileName) throws IOException {
        // TODO probably a better way to handle this
        try {
            return new File(fileName).length();
        } catch (Exception ex) {
            return getHdfsFileSystem().getStatus(new Path(fileName)).getUsed();
        }
    }

    protected void mkdir(String pathString) throws IOException {
        Path path = new Path(pathString);
        if (!getHdfsFileSystem().exists(path)) {
            getHdfsFileSystem().mkdirs(path);
        }
    }

    protected void moveFile(String sourceFileName, String destFileName) throws IOException {
        LOGGER.info("moving file " + sourceFileName + " -> " + destFileName);
        Path sourcePath = new Path(sourceFileName);
        Path destPath = new Path(destFileName);
        if (!getHdfsFileSystem().rename(sourcePath, destPath)) {
            throw new IOException("Cannot move file " + sourcePath + " to " + destPath);
        }
    }

    public OutputCollector getCollector() {
        return collector;
    }

    protected GraphVertex saveArtifact(ArtifactExtractedInfo artifactExtractedInfo) {
        Artifact artifact = saveArtifactModel(artifactExtractedInfo);
        GraphVertex artifactVertex = saveArtifactGraphVertex(artifactExtractedInfo, artifact);
        return artifactVertex;
    }

    private GraphVertex saveArtifactGraphVertex(ArtifactExtractedInfo artifactExtractedInfo, Artifact artifact) {
        if (artifactExtractedInfo.getUrl() != null && !artifactExtractedInfo.getUrl().isEmpty()) {
            artifactExtractedInfo.setSource(artifactExtractedInfo.getUrl());
        }
        GraphVertex vertex = artifactRepository.saveToGraph(artifact, artifactExtractedInfo, user);
        return vertex;
    }

    private Artifact saveArtifactModel(ArtifactExtractedInfo artifactExtractedInfo) {
        Artifact artifact = artifactRepository.findByRowKey(artifactExtractedInfo.getRowKey(), user.getModelUserContext());
        if (artifact == null) {
            artifact = new Artifact(artifactExtractedInfo.getRowKey());
            if (artifactExtractedInfo.getDate() != null) {
                artifact.getMetadata().setCreateDate(artifactExtractedInfo.getDate());
            } else {
                artifact.getMetadata().setCreateDate(new Date());
            }
        }
        if (artifactExtractedInfo.getRaw() != null) {
            artifact.getMetadata().setRaw(artifactExtractedInfo.getRaw());
        }
        if (artifactExtractedInfo.getVideoTranscript() != null) {
            artifact.getMetadata().setVideoTranscript(artifactExtractedInfo.getVideoTranscript());
            artifact.getMetadata().setVideoDuration(Long.toString(artifactExtractedInfo.getVideoDuration()));

            // TODO should we combine text like this? If the text ends up on HDFS the text here is technically invalid
            if (artifactExtractedInfo.getText() == null) {
                artifactExtractedInfo.setText(artifactExtractedInfo.getVideoTranscript().toString());
            } else {
                artifactExtractedInfo.setText(artifactExtractedInfo.getText() + "\n\n" + artifactExtractedInfo.getVideoTranscript().toString());
            }
        }
        if (artifactExtractedInfo.getText() != null) {
            artifact.getMetadata().setText(artifactExtractedInfo.getText());
            if (artifact.getMetadata().getHighlightedText() == null) {
                artifact.getMetadata().setHighlightedText(artifactExtractedInfo.getText());
            }
        }
        if (artifactExtractedInfo.getMappingJson() != null) {
            artifact.getMetadata().setMappingJson(artifactExtractedInfo.getMappingJson());
        }
        if (artifactExtractedInfo.getTitle() != null) {
            artifact.getMetadata().setFileName(artifactExtractedInfo.getTitle());
        }
        if (artifactExtractedInfo.getFileExtension() != null) {
            artifact.getMetadata().setFileExtension(artifactExtractedInfo.getFileExtension());
        }
        if (artifactExtractedInfo.getMimeType() != null) {
            artifact.getMetadata().setMimeType(artifactExtractedInfo.getMimeType());
        }

        artifactRepository.save(artifact, getUser().getModelUserContext());
        return artifact;
    }

    protected User getUser() {
        return user;
    }

    @Inject
    public void setArtifactRepository(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    @Inject
    public void setGraphRepository(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    @Inject
    public void setAuditRepository(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public void setUser (User user) { this.user = user; }

    @Inject
    public void setTermMentionRepository(TermMentionRepository termMentionRepository) {
        this.termMentionRepository = termMentionRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public void setMetricsManager(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }

    protected boolean isArchive(final String fileName) {
        String extension = FileUtils.getExtension(fileName.toLowerCase());
        return ARCHIVE_EXTENSIONS.contains(extension);
    }

    public static String getFileNameWithDateSuffix(String fileName) {
        return FilenameUtils.getName(fileName) + "__" + fileNameSuffix.format(new Date());
    }
}
