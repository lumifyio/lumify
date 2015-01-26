package io.lumify.mimeTypeOntologyMapper;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Element;
import org.securegraph.Property;

import java.io.InputStream;

public class MimeTypeOntologyMapperGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MimeTypeOntologyMapperGraphPropertyWorker.class);
    private Concept imageConcept;
    private Concept audioConcept;
    private Concept videoConcept;
    private Concept documentConcept;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        imageConcept = getOntologyRepository().getRequiredConceptByIntent("image");
        audioConcept = getOntologyRepository().getRequiredConceptByIntent("audio");
        videoConcept = getOntologyRepository().getRequiredConceptByIntent("video");
        documentConcept = getOntologyRepository().getRequiredConceptByIntent("document");
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = LumifyProperties.MIME_TYPE.getPropertyValue(data.getElement());
        Concept concept = null;

        if (imageConcept != null && mimeType.startsWith("image")) {
            concept = imageConcept;
        } else if (audioConcept != null && mimeType.startsWith("audio")) {
            concept = audioConcept;
        } else if (videoConcept != null && mimeType.startsWith("video")) {
            concept = videoConcept;
        } else if (documentConcept != null) {
            concept = documentConcept;
        }

        if (concept == null) {
            LOGGER.debug("skipping, no concept mapped for vertex " + data.getElement().getId());
            return;
        }

        LOGGER.debug("assigning concept type %s to vertex %s", concept.getIRI(), data.getElement().getId());
        LumifyProperties.CONCEPT_TYPE.setProperty(data.getElement(), concept.getIRI(), data.createPropertyMetadata(), data.getVisibility(), getAuthorizations());
        getGraph().flush();
        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), null, LumifyProperties.CONCEPT_TYPE.getPropertyName(),
                data.getWorkspaceId(), data.getVisibilitySource());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = LumifyProperties.MIME_TYPE.getPropertyValue(element);
        if (mimeType == null) {
            return false;
        }

        String existingConceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(element);
        if (existingConceptType != null) {
            return false;
        }

        return true;
    }
}
