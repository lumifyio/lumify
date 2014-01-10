package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.lumify.core.model.GeoLocation;

public class TermMentionMetadata extends ColumnFamily {
    public static final String NAME = "Metadata";
    public static final String SIGN = "sign";
    public static final String ONTOLOGY_CLASS_URI = "ontologyClassUri";
    public static final String GRAPH_VERTEX_ID = "graphVertexId";
    public static final String CONCEPT_GRAPH_VERTEX_ID = "conceptGraphVertexId";
    public static final String GEO_LOCATION = "geoLocation";
    public static final String GEO_LOCATION_TITLE = "geoLocationTitle";
    public static final String GEO_LOCATION_POPULATION = "geoLocationPopulation";

    public TermMentionMetadata() {
        super(NAME);
    }

    public TermMentionMetadata setSign(String text) {
        set(SIGN, text);
        return this;
    }

    public String getSign() {
        return Value.toString(get(SIGN));
    }

    public TermMentionMetadata setGraphVertexId(String graphVertexId) {
        set(GRAPH_VERTEX_ID, graphVertexId);
        return this;
    }

    public String getGraphVertexId() {
        return Value.toString(get(GRAPH_VERTEX_ID));
    }

    public TermMentionMetadata setOntologyClassUri(String ontologyClassUri) {
        set(ONTOLOGY_CLASS_URI, ontologyClassUri);
        return this;
    }

    public String getOntologyClassUri() {
        return Value.toString(get(ONTOLOGY_CLASS_URI));
    }

    public TermMentionMetadata setConceptGraphVertexId(Object conceptGraphVertexId) {
        set(CONCEPT_GRAPH_VERTEX_ID, conceptGraphVertexId);
        return this;
    }

    public String getConceptGraphVertexId() {
        return Value.toString(get(CONCEPT_GRAPH_VERTEX_ID));
    }

    public String getGeoLocation() {
        return Value.toString(get(GEO_LOCATION));
    }

    public Double getLatitude() {
        return GeoLocation.getLatitude(getGeoLocation());
    }

    public Double getLongitude() {
        return GeoLocation.getLongitude(getGeoLocation());
    }

    public TermMentionMetadata setGeoLocation(String geoLocation) {
        set(GEO_LOCATION, geoLocation);
        return this;
    }

    public TermMentionMetadata setGeoLocation(Double lat, Double lon) {
        return setGeoLocation(GeoLocation.getGeoLocation(lat, lon));
    }

    public TermMentionMetadata setGeoLocationTitle(String geoLocationTitle) {
        set(GEO_LOCATION_TITLE, geoLocationTitle);
        return this;
    }

    public String getGeoLocationTitle() {
        return Value.toString(get(GEO_LOCATION_TITLE));
    }

    public TermMentionMetadata setGeoLocationPopulation(Long geoLocationPopulation) {
        set(GEO_LOCATION_POPULATION, geoLocationPopulation);
        return this;
    }

    public Long getGeoLocationPopulation() {
        return Value.toLong(get(GEO_LOCATION_POPULATION));
    }
}
