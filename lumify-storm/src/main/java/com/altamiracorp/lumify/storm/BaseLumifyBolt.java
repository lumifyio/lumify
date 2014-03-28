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
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.ingest.BaseArtifactProcessor;
import com.altamiracorp.lumify.core.metrics.JmxMetricsManager;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.codehaus.plexus.util.FileUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.SOURCE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;
import static com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties.*;
import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.*;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseLumifyBolt extends BaseRichBolt {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseLumifyBolt.class);
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
    protected OntologyRepository ontologyRepository;
    private FileSystem hdfsFileSystem;
    protected Graph graph;
    protected AuditRepository auditRepository;
    protected TermMentionRepository termMentionRepository;
    private UserRepository userRepository;
    private Injector injector;
    private Counter totalProcessedCounter;
    private Counter processingCounter;
    private Counter totalErrorCounter;
    private Timer processingTimeTimer;
    private JmxMetricsManager metricsManager;
    protected WorkQueueRepository workQueueRepository;
    private User user;
    private Authorizations authorizations;

    @Override
    public void prepare(final Map stormConf, TopologyContext context, OutputCollector collector) {
        LOGGER.info("Configuring environment for bolt: %s-%d", context.getThisComponentId(), context.getThisTaskId());
        this.collector = collector;
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(new com.altamiracorp.lumify.core.config.Configuration(stormConf)));

        String namePrefix = metricsManager.getNamePrefix(this);
        totalProcessedCounter = metricsManager.getRegistry().counter(namePrefix + "total-processed");
        processingCounter = metricsManager.getRegistry().counter(namePrefix + "processing");
        totalErrorCounter = metricsManager.getRegistry().counter(namePrefix + "total-errors");
        processingTimeTimer = metricsManager.getRegistry().timer(namePrefix + "processing-time");

        user = (User) stormConf.get("user");
        if (user == null) {
            user = userRepository.getSystemUser();
        }
        authorizations = userRepository.getAuthorizations(user);

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
            LOGGER.debug("BEGIN %s: [MessageID: %s]", getClass().getName(), input.getMessageId());
            LOGGER.trace("BEGIN %s: [MessageID: %s] %s", getClass().getName(), input.getMessageId(), input);
            try {
                safeExecute(input);
                LOGGER.debug("ACK'ing: [MessageID: %s]", input.getMessageId());
                LOGGER.trace("ACK'ing: [MessageID: %s] %s", input.getMessageId(), input);
                getCollector().ack(input);
            } catch (Exception e) {
                totalErrorCounter.inc();
                LOGGER.error("Error occurred during execution: " + input, e);
                getCollector().reportError(e);
                getCollector().fail(input);
            }

            LOGGER.debug("END %s: [MessageID: %s]", getClass().getName(), input.getMessageId());
            LOGGER.trace("END %s: [MessageID: %s] %s", getClass().getName(), input.getMessageId(), input);
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

    protected long getFileSize(String path) throws IOException {
        // TODO probably a better way to handle this
        try {
            if (getHdfsFileSystem().exists(new Path(path))) {
                return getHdfsFileSystem().getFileStatus(new Path(path)).getLen();
            } else {
                return new File(path).length();
            }
        } catch (Exception ex) {
            return getHdfsFileSystem().getStatus(new Path(path)).getUsed();
        }
    }

    protected void mkdir(String pathString) throws IOException {
        Path path = new Path(pathString);
        if (!getHdfsFileSystem().exists(path)) {
            getHdfsFileSystem().mkdirs(path);
        }
    }

    protected void moveFile(String sourceFileName, String destFileName) throws IOException {
        LOGGER.info("moving file %s -> %s", sourceFileName, destFileName);
        Path sourcePath = new Path(sourceFileName);
        Path destPath = new Path(destFileName);
        if (!getHdfsFileSystem().rename(sourcePath, destPath)) {
            throw new IOException("Cannot move file " + sourcePath + " to " + destPath);
        }
    }

    public OutputCollector getCollector() {
        return collector;
    }

    protected Vertex saveArtifact(ArtifactExtractedInfo artifactExtractedInfo) throws Exception {
        if (artifactExtractedInfo.getUrl() != null && !artifactExtractedInfo.getUrl().isEmpty()) {
            artifactExtractedInfo.setSource(artifactExtractedInfo.getUrl());
        }

        ElementMutation<Vertex> vertexMutation = BaseArtifactProcessor.findOrPrepareArtifactVertex(graph, authorizations, artifactExtractedInfo.getRowKey());
        updateMutationWithArtifactExtractedInfo(vertexMutation, artifactExtractedInfo);
        Vertex vertex = null;
        if (!(vertexMutation instanceof ExistingElementMutation)) {
            vertex = vertexMutation.save();
            auditRepository.auditVertexElementMutation(AuditAction.UPDATE, vertexMutation, vertex, artifactExtractedInfo.getProcess(), user, new Visibility(""));
        } else {
            auditRepository.auditVertexElementMutation(AuditAction.UPDATE, vertexMutation, vertex, artifactExtractedInfo.getProcess(), user, new Visibility(""));
            vertex = vertexMutation.save();
        }
        graph.flush();
        // TODO remove temp files artifactExtractedInfo.getTextHdfsPath() and artifactExtractedInfo.getRawHdfsPath()
        return vertex;
    }

    private void updateMutationWithArtifactExtractedInfo(ElementMutation<Vertex> artifact, ArtifactExtractedInfo artifactExtractedInfo) throws Exception {
        LumifyVisibility lumifyVisibility = new LumifyVisibility();

        checkNotNull(artifactExtractedInfo.getConceptType(), "concept type cannot be null");
        Concept concept = ontologyRepository.getConceptById(artifactExtractedInfo.getConceptType());
        checkNotNull(concept, "Could not find concept " + artifactExtractedInfo.getConceptType());
        CONCEPT_TYPE.setProperty(artifact, concept.getId(), lumifyVisibility.getVisibility());

        if (artifactExtractedInfo.getDate() != null) {
            CREATE_DATE.setProperty(artifact, artifactExtractedInfo.getDate(), lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getRaw() != null || artifactExtractedInfo.getRawHdfsPath() != null) {
            StreamingPropertyValue rawStreamingPropertyValue;
            if (artifactExtractedInfo.getRaw() != null) {
                rawStreamingPropertyValue = new StreamingPropertyValue(new ByteArrayInputStream(artifactExtractedInfo.getRaw()), byte[].class);
            } else {
                rawStreamingPropertyValue = new StreamingPropertyValue(openFile(artifactExtractedInfo.getRawHdfsPath()), byte[].class);
            }
            rawStreamingPropertyValue.searchIndex(false);
            RAW.setProperty(artifact, rawStreamingPropertyValue, lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getVideoTranscript() != null) {
            // TODO should video transcript be converted to a StreamingPropertyValue?
            VIDEO_TRANSCRIPT.setProperty(artifact, artifactExtractedInfo.getVideoTranscript().toJson().toString(), lumifyVisibility.getVisibility());
            VIDEO_DURATION.setProperty(artifact, artifactExtractedInfo.getVideoDuration(), lumifyVisibility.getVisibility());

            // TODO should we combine text like this? If the text ends up on HDFS the text here is technically invalid
            if (artifactExtractedInfo.getText() == null) {
                artifactExtractedInfo.setText(artifactExtractedInfo.getVideoTranscript().toString());
            } else {
                artifactExtractedInfo.setText(artifactExtractedInfo.getText() + artifactExtractedInfo.getVideoTranscript().toString());
            }
        }

        if (artifactExtractedInfo.getText() != null || artifactExtractedInfo.getTextHdfsPath() != null) {
            StreamingPropertyValue textStreamingPropertyValue;
            if (artifactExtractedInfo.getText() != null) {
                textStreamingPropertyValue = new StreamingPropertyValue(new ByteArrayInputStream(artifactExtractedInfo.getText().getBytes()), String.class);
            } else {
                textStreamingPropertyValue = new StreamingPropertyValue(openFile(artifactExtractedInfo.getTextHdfsPath()), String.class);
            }
            TEXT.setProperty(artifact, textStreamingPropertyValue, lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getMappingJson() != null) {
            MAPPING_JSON.setProperty(artifact, artifactExtractedInfo.getMappingJson(), lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getTitle() != null) {
            TITLE.setProperty(artifact, artifactExtractedInfo.getTitle(), lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getFileName() != null) {
            FILE_NAME.setProperty(artifact, artifactExtractedInfo.getFileName(), lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getFileExtension() != null) {
            FILE_NAME_EXTENSION.setProperty(artifact, artifactExtractedInfo.getFileExtension(), lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getMimeType() != null) {
            MIME_TYPE.setProperty(artifact, artifactExtractedInfo.getMimeType(), lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getSource() != null) {
            SOURCE.setProperty(artifact, artifactExtractedInfo.getSource(), lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getDate() != null) {
            PUBLISHED_DATE.setProperty(artifact, artifactExtractedInfo.getDate(), lumifyVisibility.getVisibility());
        }

        String author = artifactExtractedInfo.getAuthor();
        if (author != null && !author.trim().isEmpty()) {
            AUTHOR.setProperty(artifact, author.trim(), lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getMp4HdfsFilePath() != null) {
            StreamingPropertyValue spv = new StreamingPropertyValue(openFile(artifactExtractedInfo.getMp4HdfsFilePath()), byte[].class);
            spv.searchIndex(false);
            getVideoProperty(VIDEO_TYPE_MP4).setProperty(artifact, spv, lumifyVisibility.getVisibility());
            getVideoSizeProperty(VIDEO_TYPE_MP4).setProperty(artifact, getFileSize(artifactExtractedInfo.getMp4HdfsFilePath()), lumifyVisibility.getVisibility());
        }
        if (artifactExtractedInfo.getWebMHdfsFilePath() != null) {
            StreamingPropertyValue spv = new StreamingPropertyValue(openFile(artifactExtractedInfo.getWebMHdfsFilePath()), byte[].class);
            spv.searchIndex(false);
            getVideoProperty(VIDEO_TYPE_WEBM).setProperty(artifact, spv, lumifyVisibility.getVisibility());
            getVideoSizeProperty(VIDEO_TYPE_WEBM).setProperty(artifact, getFileSize(artifactExtractedInfo.getWebMHdfsFilePath()),
                    lumifyVisibility.getVisibility());
        }
        if (artifactExtractedInfo.getAudioHdfsPath() != null) {
            StreamingPropertyValue spv = new StreamingPropertyValue(openFile(artifactExtractedInfo.getAudioHdfsPath()), byte[].class);
            spv.searchIndex(false);
            AUDIO.setProperty(artifact, spv, lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getAudioMp4HdfsFilePath() != null) {
            StreamingPropertyValue spv = new StreamingPropertyValue(openFile(artifactExtractedInfo.getAudioMp4HdfsFilePath()), byte[].class);
            spv.searchIndex(false);
            getAudioProperty(AUDIO_TYPE_MP4).setProperty(artifact, spv, lumifyVisibility.getVisibility());
            getAudioSizeProperty(AUDIO_TYPE_MP4).setProperty(artifact, getFileSize(artifactExtractedInfo.getAudioMp4HdfsFilePath()), lumifyVisibility.getVisibility());
        }
        if (artifactExtractedInfo.getAudioOggHdfsFilePath() != null) {
            StreamingPropertyValue spv = new StreamingPropertyValue(openFile(artifactExtractedInfo.getAudioOggHdfsFilePath()), byte[].class);
            spv.searchIndex(false);
            getAudioProperty(AUDIO_TYPE_OGG).setProperty(artifact, spv, lumifyVisibility.getVisibility());
            getAudioSizeProperty(AUDIO_TYPE_OGG).setProperty(artifact, getFileSize(artifactExtractedInfo.getAudioOggHdfsFilePath()),
                    lumifyVisibility.getVisibility());
        }

        if (artifactExtractedInfo.getPosterFrameHdfsPath() != null) {
            StreamingPropertyValue spv = new StreamingPropertyValue(openFile(artifactExtractedInfo.getPosterFrameHdfsPath()), byte[].class);
            spv.searchIndex(false);
            RAW_POSTER_FRAME.setProperty(artifact, spv, lumifyVisibility.getVisibility());
        }
    }

    protected User getUser() {
        return user;
    }

    protected Authorizations getAuthorizations() {
        return authorizations;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
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
    public void setTermMentionRepository(TermMentionRepository termMentionRepository) {
        this.termMentionRepository = termMentionRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public void setMetricsManager(JmxMetricsManager metricsManager) {
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
