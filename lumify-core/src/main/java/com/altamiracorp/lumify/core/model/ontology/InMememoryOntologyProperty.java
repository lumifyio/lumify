package com.altamiracorp.lumify.core.model.ontology;

public class InMememoryOntologyProperty extends OntologyProperty {
    private String title;
    private boolean userVisible;
    private String displayName;
    private PropertyType dataType;

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
}
