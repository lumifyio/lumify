package io.lumify.securegraph.model.ontology;

import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.PossibleValueType;
import io.lumify.core.model.ontology.PropertyType;
import org.apache.commons.lang.SerializationUtils;
import org.securegraph.Vertex;

import java.util.ArrayList;
import java.util.List;

import static io.lumify.core.model.ontology.OntologyLumifyProperties.*;
import static io.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;

public class SecureGraphOntologyProperty extends OntologyProperty {
    private final Vertex vertex;

    public SecureGraphOntologyProperty(Vertex vertex) {
        this.vertex = vertex;
    }

    public String getTitle() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return DISPLAY_NAME.getPropertyValue(vertex);
    }

    public boolean getUserVisible() {
        return USER_VISIBLE.getPropertyValue(vertex);
    }

    public boolean getSearchable () { return SEARCHABLE.getPropertyValue(vertex); }

    public PropertyType getDataType() {
        return PropertyType.convert(DATA_TYPE.getPropertyValue(vertex));
    }

    public List<PossibleValueType> getPossibleValues() {
        final byte[] possibleValues = POSSIBLE_VALUES.getPropertyValue(vertex);
        return possibleValues == null ? new ArrayList<PossibleValueType>() : (ArrayList<PossibleValueType>) SerializationUtils.deserialize(possibleValues);
    }

    public Vertex getVertex() {
        return this.vertex;
    }
}
