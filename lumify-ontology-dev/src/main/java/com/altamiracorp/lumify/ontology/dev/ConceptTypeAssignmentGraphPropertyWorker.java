package com.altamiracorp.lumify.ontology.dev;

import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.mutation.ElementMutation;

import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class ConceptTypeAssignmentGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ConceptTypeAssignmentGraphPropertyWorker.class);

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = RawLumifyProperties.MIME_TYPE.getPropertyValue(data.getVertex());
        Concept concept;

        if (mimeType.startsWith("image")) {
            concept = getOntologyRepository().getConceptByIRI(ConceptType.IMAGE.toString());
            checkNotNull(concept, "Could not find concept " + ConceptType.IMAGE.toString());
        } else if (mimeType.startsWith("audio")) {
            concept = getOntologyRepository().getConceptByIRI(ConceptType.AUDIO.toString());
            checkNotNull(concept, "Could not find concept " + ConceptType.AUDIO.toString());
        } else if (mimeType.startsWith("video")) {
            concept = getOntologyRepository().getConceptByIRI(ConceptType.VIDEO.toString());
            checkNotNull(concept, "Could not find concept " + ConceptType.VIDEO.toString());
        } else {
            concept = getOntologyRepository().getConceptByIRI(ConceptType.DOCUMENT.toString());
            checkNotNull(concept, "Could not find concept " + ConceptType.DOCUMENT.toString());
        }

        LOGGER.debug("assigning concept type %s to vertex %s", concept.getTitle(), data.getVertex().getId());
        OntologyLumifyProperties.CONCEPT_TYPE.setProperty(data.getVertex(), concept.getTitle(), data.getVertex().getVisibility());
        getGraph().flush();
        getWorkQueueRepository().pushGraphPropertyQueue(data.getVertex().getId(), ElementMutation.DEFAULT_KEY, OntologyLumifyProperties.CONCEPT_TYPE.getKey());
    }

    @Override
    public boolean isHandled(Vertex vertex, Property property) {
        if (!property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }

        String mimeType = RawLumifyProperties.MIME_TYPE.getPropertyValue(vertex);
        return mimeType != null;
    }
}
