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

import static org.securegraph.util.Preconditions.checkNotNull;

public class MimeTypeOntologyMapperGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MimeTypeOntologyMapperGraphPropertyWorker.class);
    public static final String CONFIG_ONTOLOGY_IRI_IMAGE = "ontology.iri.image";
    public static final String CONFIG_ONTOLOGY_IRI_AUDIO = "ontology.iri.audio";
    public static final String CONFIG_ONTOLOGY_IRI_VIDEO = "ontology.iri.video";
    public static final String CONFIG_ONTOLOGY_IRI_DOCUMENT = "ontology.iri.document";
    private Concept imageConcept;
    private Concept audioConcept;
    private Concept videoConcept;
    private Concept documentConcept;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        String imageIri = getConfiguration().get(CONFIG_ONTOLOGY_IRI_IMAGE, null);
        if (imageIri != null) {
            imageConcept = getOntologyRepository().getConceptByIRI(imageIri);
            checkNotNull(imageConcept, "Could not find concept (" + CONFIG_ONTOLOGY_IRI_IMAGE + ")" + imageIri);
        }

        String audioIri = getConfiguration().get(CONFIG_ONTOLOGY_IRI_AUDIO, null);
        if (audioIri != null) {
            audioConcept = getOntologyRepository().getConceptByIRI(audioIri);
            checkNotNull(audioConcept, "Could not find concept (" + CONFIG_ONTOLOGY_IRI_AUDIO + ")" + audioIri);
        }

        String videoIri = getConfiguration().get(CONFIG_ONTOLOGY_IRI_VIDEO, null);
        if (videoIri != null) {
            videoConcept = getOntologyRepository().getConceptByIRI(videoIri);
            checkNotNull(videoConcept, "Could not find concept (" + CONFIG_ONTOLOGY_IRI_VIDEO + ")" + videoIri);
        }

        String documentIri = getConfiguration().get(CONFIG_ONTOLOGY_IRI_DOCUMENT, null);
        if (documentIri != null) {
            documentConcept = getOntologyRepository().getConceptByIRI(documentIri);
            checkNotNull(documentConcept, "Could not find concept (" + CONFIG_ONTOLOGY_IRI_DOCUMENT + ")" + documentIri);
        }
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

        LOGGER.debug("assigning concept type %s to vertex %s", concept.getTitle(), data.getElement().getId());
        LumifyProperties.CONCEPT_TYPE.setProperty(data.getElement(), concept.getTitle(), data.createPropertyMetadata(), data.getVisibility(), getAuthorizations());
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
