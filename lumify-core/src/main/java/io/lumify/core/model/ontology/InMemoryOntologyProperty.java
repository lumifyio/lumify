package io.lumify.core.model.ontology;

import java.util.ArrayList;

public class InMemoryOntologyProperty extends OntologyProperty {
    private String title;
    private boolean userVisible;
    private String displayName;
    private PropertyType dataType;
    private ArrayList<PossibleValueType> possibleValues;

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
    public ArrayList<PossibleValueType> getPossibleValues() {
        return possibleValues;
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

    public void setPossibleValues(ArrayList<PossibleValueType> possibleValues) {
        this.possibleValues = possibleValues;
    }
}
