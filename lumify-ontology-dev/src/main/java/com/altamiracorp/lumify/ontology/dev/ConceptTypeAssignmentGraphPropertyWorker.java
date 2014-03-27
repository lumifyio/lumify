package com.altamiracorp.lumify.ontology.dev;

import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkResult;
import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.google.inject.Inject;

import java.io.InputStream;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.google.common.base.Preconditions.checkNotNull;

public class ConceptTypeAssignmentGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ConceptTypeAssignmentGraphPropertyWorker.class);
    private OntologyRepository ontologyRepository;

    @Override
    public GraphPropertyWorkResult execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = RawLumifyProperties.MIME_TYPE.getPropertyValue(data.getVertex());
        Concept concept;

        if (mimeType.startsWith("image")) {
            concept = ontologyRepository.getConceptById(ConceptType.IMAGE.toString());
            checkNotNull(concept, "Could not find concept " + ConceptType.IMAGE.toString());
        } else if (mimeType.startsWith("audio")) {
            concept = ontologyRepository.getConceptById(ConceptType.AUDIO.toString());
            checkNotNull(concept, "Could not find concept " + ConceptType.AUDIO.toString());
        } else if (mimeType.startsWith("video")) {
            concept = ontologyRepository.getConceptById(ConceptType.VIDEO.toString());
            checkNotNull(concept, "Could not find concept " + ConceptType.VIDEO.toString());
        } else {
            concept = ontologyRepository.getConceptById(ConceptType.DOCUMENT.toString());
            checkNotNull(concept, "Could not find concept " + ConceptType.DOCUMENT.toString());
        }

        LOGGER.debug("assigning concept type %s to vertex %s", concept.getId(), data.getVertex().getId());
        CONCEPT_TYPE.setProperty(data.getVertex(), concept.getId(), data.getVertex().getVisibility());
        getGraph().flush();
        getWorkQueueRepository().pushGraphPropertyQueue(data.getVertex().getId(), ElementMutation.DEFAULT_KEY, CONCEPT_TYPE.getKey());

        return new GraphPropertyWorkResult();
    }

    @Override
    public boolean isHandled(Vertex vertex, Property property) {
        if (!property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }

        String mimeType = RawLumifyProperties.MIME_TYPE.getPropertyValue(vertex);
        return mimeType != null;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }
}
