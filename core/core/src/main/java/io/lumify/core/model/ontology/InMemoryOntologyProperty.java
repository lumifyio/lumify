package io.lumify.core.model.ontology;

import org.json.JSONObject;

public class InMemoryOntologyProperty extends OntologyProperty {
    private String title;
    private boolean userVisible;
    private boolean searchable;
    private String displayName;
    private PropertyType dataType;
    private JSONObject possibleValues;
    private Boolean displayTime;
    private Double boost;

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
    public JSONObject getPossibleValues() {
        return possibleValues;
    }

    @Override
    public boolean getSearchable() {
        return searchable;
    }

    @Override
    public Boolean getDisplayTime() {
        return displayTime;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDisplayTime(Boolean displayTime) {
        this.displayTime = displayTime;
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

    public void setPossibleValues(JSONObject possibleValues) {
        this.possibleValues = possibleValues;
    }

    public void setBoost(Double boost) {
        this.boost = boost;
    }
}
