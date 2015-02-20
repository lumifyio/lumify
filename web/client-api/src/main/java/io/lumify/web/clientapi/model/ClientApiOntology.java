package io.lumify.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

public class ClientApiOntology implements ClientApiObject {
    private List<Concept> concepts = new ArrayList<>();
    private List<Property> properties = new ArrayList<>();
    private List<Relationship> relationships = new ArrayList<>();

    public List<Concept> getConcepts() {
        return concepts;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public void addAllConcepts(Collection<Concept> concepts) {
        this.concepts.addAll(concepts);
    }

    public void addAllProperties(Collection<Property> properties) {
        this.properties.addAll(properties);
    }

    public void addAllRelationships(Collection<Relationship> relationships) {
        this.relationships.addAll(relationships);
    }

    public static class Concept {
        private String id;
        private String title;
        private String displayName;
        private String displayType;
        private String titleFormula;
        private String subtitleFormula;
        private String timeFormula;
        private String parentConcept;
        private String pluralDisplayName;
        private Boolean userVisible;
        private Boolean searchable;
        private String glyphIconHref;
        private String color;
        private List<String> intents = new ArrayList<>();
        private List<String> addRelatedConceptWhiteList = new ArrayList<>();
        private List<String> properties = new ArrayList<>();
        private Map<String, String> metadata = new HashMap<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayType() {
            return displayType;
        }

        public void setDisplayType(String displayType) {
            this.displayType = displayType;
        }

        public String getTitleFormula() {
            return titleFormula;
        }

        public void setTitleFormula(String titleFormula) {
            this.titleFormula = titleFormula;
        }

        public String getSubtitleFormula() {
            return subtitleFormula;
        }

        public void setSubtitleFormula(String subtitleFormula) {
            this.subtitleFormula = subtitleFormula;
        }

        public String getTimeFormula() {
            return timeFormula;
        }

        public void setTimeFormula(String timeFormula) {
            this.timeFormula = timeFormula;
        }

        public String getParentConcept() {
            return parentConcept;
        }

        public void setParentConcept(String parentConcept) {
            this.parentConcept = parentConcept;
        }

        public String getPluralDisplayName() {
            return pluralDisplayName;
        }

        public void setPluralDisplayName(String pluralDisplayName) {
            this.pluralDisplayName = pluralDisplayName;
        }

        public Boolean getUserVisible() {
            return userVisible;
        }

        public void setUserVisible(Boolean userVisible) {
            this.userVisible = userVisible;
        }

        public Boolean getSearchable() {
            return searchable;
        }

        public void setSearchable(Boolean searchable) {
            this.searchable = searchable;
        }

        public String getGlyphIconHref() {
            return glyphIconHref;
        }

        public void setGlyphIconHref(String glyphIconHref) {
            this.glyphIconHref = glyphIconHref;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Map<String, String> getMetadata() {
            return metadata;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<String> getAddRelatedConceptWhiteList() {
            return addRelatedConceptWhiteList;
        }

        public List<String> getProperties() {
            return properties;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<String> getIntents() {
            return intents;
        }
    }

    public static class Property {
        private String title;
        private String displayName;
        private boolean userVisible;
        private boolean searchable;
        private boolean addable;
        private PropertyType dataType;
        private String displayType;
        private String propertyGroup;
        private Map<String, String> possibleValues = new HashMap<>();
        private String validationFormula;
        private String displayFormula;
        private String[] dependentPropertyIris;
        private List<String> intents = new ArrayList<>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public boolean isUserVisible() {
            return userVisible;
        }

        public void setUserVisible(boolean userVisible) {
            this.userVisible = userVisible;
        }

        public boolean isSearchable() {
            return searchable;
        }

        public void setSearchable(boolean searchable) {
            this.searchable = searchable;
        }

        public boolean isAddable() {
            return addable;
        }

        public void setAddable(boolean addable) {
            this.addable = addable;
        }

        public PropertyType getDataType() {
            return dataType;
        }

        public void setDataType(PropertyType dataType) {
            this.dataType = dataType;
        }

        public String getDisplayType() {
            return displayType;
        }

        public void setDisplayType(String displayType) {
            this.displayType = displayType;
        }

        public String getPropertyGroup() {
            return propertyGroup;
        }

        public void setPropertyGroup(String propertyGroup) {
            this.propertyGroup = propertyGroup;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Map<String, String> getPossibleValues() {
            return possibleValues;
        }

        public void setValidationFormula(String validationFormula) {
            this.validationFormula = validationFormula;
        }

        public String getValidationFormula() {
            return validationFormula;
        }

        public void setDisplayFormula(String displayFormula) {
            this.displayFormula = displayFormula;
        }

        public String getDisplayFormula() {
            return displayFormula;
        }

        public void setDependentPropertyIris(String[] dependentPropertyIris) {
            this.dependentPropertyIris = dependentPropertyIris;
        }

        public void setDependentPropertyIris(Collection<String> dependentPropertyIris) {
            if (dependentPropertyIris == null || dependentPropertyIris.size() == 0) {
                this.dependentPropertyIris = null;
            } else {
                this.dependentPropertyIris = dependentPropertyIris.toArray(new String[dependentPropertyIris.size()]);
            }
        }

        public String[] getDependentPropertyIris() {
            return dependentPropertyIris;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<String> getIntents() {
            return intents;
        }
    }

    public static class Relationship {
        private String title;
        private String displayName;
        private Boolean userVisible;
        private List<String> domainConceptIris = new ArrayList<>();
        private List<String> rangeConceptIris = new ArrayList<>();
        private List<InverseOf> inverseOfs = new ArrayList<>();
        private List<String> intents = new ArrayList<>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<String> getDomainConceptIris() {
            return domainConceptIris;
        }

        public void setDomainConceptIris(List<String> domainConceptIris) {
            this.domainConceptIris = domainConceptIris;
        }

        public List<String> getRangeConceptIris() {
            return rangeConceptIris;
        }

        public void setRangeConceptIris(List<String> rangeConceptIris) {
            this.rangeConceptIris = rangeConceptIris;
        }

        public Boolean getUserVisible() {
            return userVisible;
        }

        public void setUserVisible(Boolean userVisible) {
            this.userVisible = userVisible;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<InverseOf> getInverseOfs() {
            return inverseOfs;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<String> getIntents() {
            return intents;
        }

        public static class InverseOf {
            private String iri;
            private String primaryIri;

            public String getIri() {
                return iri;
            }

            public void setIri(String iri) {
                this.iri = iri;
            }

            public String getPrimaryIri() {
                return primaryIri;
            }

            public void setPrimaryIri(String primaryIri) {
                this.primaryIri = primaryIri;
            }
        }
    }
}
