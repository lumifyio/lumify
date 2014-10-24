package io.lumify.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

public class ClientApiOntology implements ClientApiObject {
    private List<Concept> concepts = new ArrayList<Concept>();
    private List<Property> properties = new ArrayList<Property>();
    private List<Relationship> relationships = new ArrayList<Relationship>();

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
        private List<String> addRelatedConceptWhiteList = new ArrayList<String>();
        private List<String> properties = new ArrayList<String>();
        private Map<String, String> metadata = new HashMap<String, String>();

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
    }

    public static class Property {
        private String title;
        private String displayName;
        private boolean userVisible;
        private boolean searchable;
        private PropertyType dataType;
        private String displayType;
        private String propertyGroup;
        private Map<String, String> possibleValues = new HashMap<String, String>();

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
    }

    public static class Relationship {
        private String title;
        private String displayName;
        private List<String> domainConceptIris = new ArrayList<String>();
        private List<String> rangeConceptIris = new ArrayList<String>();
        private List<InverseOf> inverseOfs = new ArrayList<InverseOf>();

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

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<InverseOf> getInverseOfs() {
            return inverseOfs;
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
