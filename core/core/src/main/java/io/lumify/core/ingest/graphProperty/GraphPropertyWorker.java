package io.lumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.ingest.video.VideoTranscript;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.RowKeyHelper;
import org.securegraph.*;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public abstract class GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphPropertyWorker.class);
    private Graph graph;
    private VisibilityTranslator visibilityTranslator;
    private WorkQueueRepository workQueueRepository;
    private OntologyRepository ontologyRepository;
    private AuditRepository auditRepository;
    private AuthorizationRepository authorizationRepository;
    private GraphPropertyWorkerPrepareData workerPrepareData;
    private Configuration configuration;
    private WorkspaceRepository workspaceRepository;

    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.workerPrepareData = workerPrepareData;
    }

    protected void applyTermMentionFilters(Vertex sourceVertex, Iterable<Vertex> termMentions) {
        for (TermMentionFilter termMentionFilter : this.workerPrepareData.getTermMentionFilters()) {
            try {
                termMentionFilter.apply(sourceVertex, termMentions, this.workerPrepareData.getAuthorizations());
            } catch (Exception e) {
                LOGGER.error("Could not apply term mention filter", e);
            }
        }
        getGraph().flush();
    }

    protected void pushTextUpdated(GraphPropertyWorkData data) {
        if (data == null || data.getElement() == null) {
            return;
        }
        getWorkQueueRepository().pushTextUpdated(data.getElement().getId());
    }

    public abstract void execute(InputStream in, GraphPropertyWorkData data) throws Exception;

    public abstract boolean isHandled(Element element, Property property);

    public boolean isLocalFileRequired() {
        return false;
    }

    protected User getUser() {
        return this.workerPrepareData.getUser();
    }

    public Authorizations getAuthorizations() {
        return this.workerPrepareData.getAuthorizations();
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    protected Graph getGraph() {
        return graph;
    }

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public final void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    protected WorkspaceRepository getWorkspaceRepository() {
        return workspaceRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    protected OntologyRepository getOntologyRepository() {
        return ontologyRepository;
    }

    @Inject
    public final void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    protected AuditRepository getAuditRepository() {
        return auditRepository;
    }

    @Inject
    public final void setAuditRepository(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    public final void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    protected VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    @Inject
    public final void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }

    @Inject
    public final void setAuthorizationRepository(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    protected AuthorizationRepository getAuthorizationRepository() {
        return authorizationRepository;
    }

    /**
     * Determines if this is a property that should be analyzed by text processing tools.
     */
    protected boolean isTextProperty(Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = (String) property.getMetadata().getValue(LumifyProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("text"));
    }

    protected void addVideoTranscriptAsTextPropertiesToMutation(ExistingElementMutation<Vertex> mutation, String propertyKey, VideoTranscript videoTranscript, Metadata metadata, Visibility visibility) {
        LumifyProperties.META_DATA_MIME_TYPE.setMetadata(metadata, "text/plain", getVisibilityTranslator().getDefaultVisibility());
        for (VideoTranscript.TimedText entry : videoTranscript.getEntries()) {
            String textPropertyKey = getVideoTranscriptTimedTextPropertyKey(propertyKey, entry);
            StreamingPropertyValue value = new StreamingPropertyValue(new ByteArrayInputStream(entry.getText().getBytes()), String.class);
            LumifyProperties.TEXT.addPropertyValue(mutation, textPropertyKey, value, metadata, visibility);
        }
    }

    protected void pushVideoTranscriptTextPropertiesOnWorkQueue(Element element, String propertyKey, VideoTranscript videoTranscript) {
        for (VideoTranscript.TimedText entry : videoTranscript.getEntries()) {
            String textPropertyKey = getVideoTranscriptTimedTextPropertyKey(propertyKey, entry);
            getWorkQueueRepository().pushGraphPropertyQueue(element, textPropertyKey, LumifyProperties.TEXT.getPropertyName());
        }
    }

    private String getVideoTranscriptTimedTextPropertyKey(String propertyKey, VideoTranscript.TimedText entry) {
        String startTime = String.format("%08d", Math.max(0L, entry.getTime().getStart()));
        String endTime = String.format("%08d", Math.max(0L, entry.getTime().getEnd()));
        return propertyKey + RowKeyHelper.MINOR_FIELD_SEPARATOR + MediaLumifyProperties.VIDEO_FRAME.getPropertyName() + RowKeyHelper.MINOR_FIELD_SEPARATOR + startTime + RowKeyHelper.MINOR_FIELD_SEPARATOR + endTime;
    }

    protected void addVertexToWorkspaceIfNeeded(GraphPropertyWorkData data, Vertex vertex) {
        if (data.getWorkspaceId() == null) {
            return;
        }
        graph.flush();
        getWorkspaceRepository().updateEntityOnWorkspace(data.getWorkspaceId(), vertex.getId(), false, null, getUser());
    }
}
