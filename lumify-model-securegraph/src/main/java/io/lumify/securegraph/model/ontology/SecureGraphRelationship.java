package io.lumify.securegraph.model.ontology;

import io.lumify.core.model.ontology.Relationship;
import com.altamiracorp.securegraph.Vertex;

import static io.lumify.core.model.ontology.OntologyLumifyProperties.ONTOLOGY_TITLE;
import static io.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;

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
