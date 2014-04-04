package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.securegraph.Vertex;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.ONTOLOGY_TITLE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;

public class SecureGraphRelationship extends Relationship {
    private final Vertex vertex;

    public SecureGraphRelationship(Vertex vertex, String sourceConceptIRI, String destConceptIRI) {
        super(sourceConceptIRI, destConceptIRI);
        this.vertex = vertex;
    }

    public String getIRI() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return DISPLAY_NAME.getPropertyValue(vertex);
    }
}
