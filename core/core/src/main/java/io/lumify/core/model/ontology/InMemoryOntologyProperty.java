package io.lumify.core.model.ontology;

import com.google.common.collect.ImmutableList;
import io.lumify.web.clientapi.model.PropertyType;

import java.util.List;
import java.util.Map;

public class InMemoryOntologyProperty extends OntologyProperty {
    private String title;
    private boolean userVisible;
    private boolean searchable;
    private String displayName;
    private String propertyGroup;
    private PropertyType dataType;
    private Map<String, String> possibleValues;
    private String displayType;
    private Double boost;
    private String validationFormula;
    private String displayFormula;
    private ImmutableList<String> dependentPropertyIris;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean getUserVisible() {
        return userVisible;
    }

    @Override
    public PropertyType getDataType() {
        return dataType;
    }

    @Override
    public Double getBoost() {
        return boost;
    }

    @Override
    public Map<String, String> getPossibleValues() {
        return possibleValues;
    }

    @Override
    public String getPropertyGroup() {
        return propertyGroup;
    }

    @Override
    public boolean getSearchable() {
        return searchable;
    }

    @Override
    public String getValidationFormula() {
        return validationFormula;
    }

    @Override
    public String getDisplayFormula() {
        return displayFormula;
    }

    @Override
    public ImmutableList<String> getDependentPropertyIris() {
        return dependentPropertyIris;
    }

    public String getDisplayType() {
        return displayType;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUserVisible(boolean userVisible) {
        this.userVisible = userVisible;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDataType(PropertyType dataType) {
        this.dataType = dataType;
    }

    public void setPossibleValues(Map<String, String> possibleValues) {
        this.possibleValues = possibleValues;
    }

    public void setBoost(Double boost) {
        this.boost = boost;
    }

    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    public void setPropertyGroup(String propertyGroup) {
        this.propertyGroup = propertyGroup;
    }

    public void setValidationFormula(String validationFormula) {
        this.validationFormula = validationFormula;
    }

    public void setDisplayFormula(String displayFormula) {
        this.displayFormula = displayFormula;
    }

    public void setDependentPropertyIris(ImmutableList<String> dependentPropertyIris) {
        this.dependentPropertyIris = dependentPropertyIris;
    }
}
