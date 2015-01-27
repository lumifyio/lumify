package io.lumify.securegraph.model.ontology;

import com.google.common.collect.ImmutableList;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.JSONUtil;
import io.lumify.web.clientapi.model.PropertyType;
import org.json.JSONObject;
import org.securegraph.Vertex;
import org.securegraph.util.IterableUtils;

import java.util.Map;

public class SecureGraphOntologyProperty extends OntologyProperty {
    private final Vertex vertex;

    public SecureGraphOntologyProperty(Vertex vertex) {
        this.vertex = vertex;
    }

    public String getTitle() {
        return LumifyProperties.ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return LumifyProperties.DISPLAY_NAME.getPropertyValue(vertex);
    }

    public String getPropertyGroup() {
        return LumifyProperties.PROPERTY_GROUP.getPropertyValue(vertex);
    }

    @Override
    public String getValidationFormula() {
        return LumifyProperties.VALIDATION_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public String getDisplayFormula() {
        return LumifyProperties.DISPLAY_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public ImmutableList<String> getDependentPropertyIris() {
        Iterable<String> dependentPropertyIris = LumifyProperties.DEPENDENT_PROPERTY_IRI.getPropertyValues(vertex);
        return ImmutableList.copyOf(dependentPropertyIris);
    }

    public String[] getIntents() {
        return IterableUtils.toArray(LumifyProperties.INTENT.getPropertyValues(vertex), String.class);
    }

    public boolean getUserVisible() {
        return LumifyProperties.USER_VISIBLE.getPropertyValue(vertex);
    }

    public boolean getSearchable() {
        return LumifyProperties.SEARCHABLE.getPropertyValue(vertex);
    }

    public PropertyType getDataType() {
        return PropertyType.convert(LumifyProperties.DATA_TYPE.getPropertyValue(vertex));
    }

    public String getDisplayType() {
        return LumifyProperties.DISPLAY_TYPE.getPropertyValue(vertex);
    }

    @Override
    public Double getBoost() {
        return LumifyProperties.BOOST.getPropertyValue(vertex);
    }

    public Map<String, String> getPossibleValues() {
        JSONObject propertyValue = LumifyProperties.POSSIBLE_VALUES.getPropertyValue(vertex);
        if (propertyValue == null) {
            return null;
        }
        return JSONUtil.toMap(propertyValue);
    }

    public Vertex getVertex() {
        return this.vertex;
    }
}
