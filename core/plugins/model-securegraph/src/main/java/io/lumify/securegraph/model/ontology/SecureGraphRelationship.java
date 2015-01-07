package io.lumify.securegraph.model.ontology;

import io.lumify.core.model.ontology.Relationship;
import org.securegraph.Vertex;

import java.util.List;

import static io.lumify.core.model.properties.LumifyProperties.ONTOLOGY_TITLE;
import static io.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;

public class SecureGraphRelationship extends Relationship {
    private final Vertex vertex;
    private final List<String> inverseOfIRIs;

    public SecureGraphRelationship(Vertex vertex, List<String> domainConceptIRIs, List<String> rangeConceptIRIs, List<String> inverseOfIRIs) {
        super(domainConceptIRIs, rangeConceptIRIs);
        this.vertex = vertex;
        this.inverseOfIRIs = inverseOfIRIs;
    }

    public String getIRI() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return DISPLAY_NAME.getPropertyValue(vertex);
    }

    @Override
    public Iterable<String> getInverseOfIRIs() {
        return inverseOfIRIs;
    }

    public Vertex getVertex() {
        return vertex;
    }
}
