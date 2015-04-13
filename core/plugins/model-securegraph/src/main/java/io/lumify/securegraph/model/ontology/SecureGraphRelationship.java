package io.lumify.securegraph.model.ontology;

import io.lumify.core.model.ontology.Relationship;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Vertex;
import org.securegraph.util.IterableUtils;

import java.util.List;

import static io.lumify.core.model.properties.LumifyProperties.*;

public class SecureGraphRelationship extends Relationship {
    private final Vertex vertex;
    private final List<String> inverseOfIRIs;

    public SecureGraphRelationship(Vertex vertex, List<String> domainConceptIRIs, List<String> rangeConceptIRIs, List<String> inverseOfIRIs) {
        super(domainConceptIRIs, rangeConceptIRIs);
        this.vertex = vertex;
        this.inverseOfIRIs = inverseOfIRIs;
    }

    @Override
    public String[] getIntents() {
        return IterableUtils.toArray(LumifyProperties.INTENT.getPropertyValues(vertex), String.class);
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

    @Override
    public boolean getUserVisible() {
        return USER_VISIBLE.getPropertyValue(vertex, true);
    }

    public Vertex getVertex() {
        return vertex;
    }
}
