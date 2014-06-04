package io.lumify.tikaMimeType;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.security.LumifyVisibilityProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Map;

public class MimeTypeGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MimeTypeGraphPropertyWorker.class);
    private TikaMimeTypeMapper mimeTypeMapper;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        mimeTypeMapper = new TikaMimeTypeMapper();
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String fileName = RawLumifyProperties.FILE_NAME.getPropertyValue(data.getElement());
        String mimeType = mimeTypeMapper.guessMimeType(in, fileName);
        if (mimeType == null) {
            return;
        }

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        Map<String, Object> mimeTypeMetadata = data.getPropertyMetadata();
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(data.getElement());
        if (visibilityJson != null) {
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setMetadata(mimeTypeMetadata, visibilityJson);
        }
        RawLumifyProperties.MIME_TYPE.setProperty(m, mimeType, mimeTypeMetadata, data.getVisibility());
        m.alterPropertyMetadata(data.getProperty(), RawLumifyProperties.MIME_TYPE.getKey(), mimeType);
        Vertex v = m.save(getAuthorizations());
        getAuditRepository().auditVertexElementMutation(AuditAction.UPDATE, m, v, MimeTypeGraphPropertyWorker.class.getName(), getUser(), data.getVisibility());
        getAuditRepository().auditAnalyzedBy(AuditAction.ANALYZED_BY, v, getClass().getSimpleName(), getUser(), v.getVisibility());

        getGraph().flush();
        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), data.getProperty());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }
        if (RawLumifyProperties.MIME_TYPE.getPropertyValue(element) != null) {
            return false;
        }

        String fileName = RawLumifyProperties.FILE_NAME.getPropertyValue(element);
        if (fileName == null) {
            return false;
        }

        return true;
    }
}
