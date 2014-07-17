package io.lumify.core.ingest.graphProperty;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibilityProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public abstract class MimeTypeGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MimeTypeGraphPropertyWorker.class);
    private Collection<PostMimeTypeWorker> postMimeTypeWorkers;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        postMimeTypeWorkers = InjectHelper.getInjectedServices(PostMimeTypeWorker.class);
        for (PostMimeTypeWorker postMimeTypeWorker : postMimeTypeWorkers) {
            try {
                postMimeTypeWorker.prepare(workerPrepareData);
            } catch (Exception ex) {
                throw new LumifyException("Could not prepare post mime type worker " + postMimeTypeWorker.getClass().getName(), ex);
            }
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }
        if (LumifyProperties.MIME_TYPE.getPropertyValue(element) != null) {
            return false;
        }

        return true;
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String fileName = LumifyProperties.FILE_NAME.getPropertyValue(data.getElement());
        String mimeType = getMimeType(in, fileName);
        if (mimeType == null) {
            return;
        }

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        Map<String, Object> mimeTypeMetadata = data.createPropertyMetadata();
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(data.getElement());
        if (visibilityJson != null) {
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setMetadata(mimeTypeMetadata, visibilityJson);
        }
        LumifyProperties.MIME_TYPE.setProperty(m, mimeType, mimeTypeMetadata, data.getVisibility());
        m.alterPropertyMetadata(data.getProperty(), LumifyProperties.MIME_TYPE.getPropertyName(), mimeType);
        Vertex v = m.save(getAuthorizations());
        getAuditRepository().auditVertexElementMutation(AuditAction.UPDATE, m, v, this.getClass().getName(), getUser(), data.getVisibility());
        getAuditRepository().auditAnalyzedBy(AuditAction.ANALYZED_BY, v, getClass().getSimpleName(), getUser(), v.getVisibility());

        getGraph().flush();

        runPostMimeTypeWorkers(mimeType, data);

        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), data.getProperty());
    }

    private void runPostMimeTypeWorkers(String mimeType, GraphPropertyWorkData data) {
        for (PostMimeTypeWorker postMimeTypeWorker : postMimeTypeWorkers) {
            try {
                LOGGER.debug("running PostMimeTypeWorker: %s on element: %s, mimeType: %s", postMimeTypeWorker.getClass().getName(), data.getElement().getId(), mimeType);
                postMimeTypeWorker.execute(mimeType, data, getAuthorizations());
            } catch (Exception ex) {
                throw new LumifyException("Failed running PostMimeTypeWorker " + postMimeTypeWorker.getClass().getName(), ex);
            }
        }
    }

    protected abstract String getMimeType(InputStream in, String fileName) throws Exception;
}
