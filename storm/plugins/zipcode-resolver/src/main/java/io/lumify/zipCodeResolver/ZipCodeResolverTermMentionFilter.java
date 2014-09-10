package io.lumify.zipCodeResolver;

import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.ingest.graphProperty.TermMentionFilterPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.ElementBuilder;
import org.securegraph.Vertex;
import org.securegraph.type.GeoPoint;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZipCodeResolverTermMentionFilter extends TermMentionFilter {
    private static final String MULTI_VALUE_PROPERTY_KEY = ZipCodeResolverTermMentionFilter.class.getName();
    private static final String CONFIG_ZIP_CODE_IRI = "ontology.iri.zipCode";
    private static final String CONFIG_GEO_LOCATION_IRI = "ontology.iri.geoLocation";
    private String zipCodeIri;
    private String geoLocationIri;
    private Map<String, ZipCodeEntry> zipCodesByZipCode = new HashMap<String, ZipCodeEntry>();
    private String artifactHasEntityIri;

    @Override
    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
        super.prepare(termMentionFilterPrepareData);

        prepareIris(termMentionFilterPrepareData);
        prepareZipCodeDatabase();
    }

    private void prepareZipCodeDatabase() {
        try {
            InputStreamReader reader = new InputStreamReader(this.getClass().getResourceAsStream("zipcode.csv"));
            CsvListReader csvReader = new CsvListReader(reader, CsvPreference.STANDARD_PREFERENCE);
            csvReader.read(); // skip title line

            List<String> line;
            while ((line = csvReader.read()) != null) {
                if (line.size() < 5) {
                    continue;
                }
                String zipCode = line.get(0);
                String city = line.get(1);
                String state = line.get(2);
                double latitude = Double.parseDouble(line.get(3));
                double longitude = Double.parseDouble(line.get(4));
                zipCodesByZipCode.put(zipCode, new ZipCodeEntry(zipCode, city, state, latitude, longitude));
            }
        } catch (IOException ex) {
            throw new LumifyException("Could not read zipcode.csv", ex);
        }
    }

    public void prepareIris(TermMentionFilterPrepareData termMentionFilterPrepareData) {
        zipCodeIri = (String) termMentionFilterPrepareData.getStormConf().get(CONFIG_ZIP_CODE_IRI);
        if (zipCodeIri == null || zipCodeIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_ZIP_CODE_IRI);
        }

        geoLocationIri = (String) termMentionFilterPrepareData.getStormConf().get(CONFIG_GEO_LOCATION_IRI);
        if (geoLocationIri == null || geoLocationIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_GEO_LOCATION_IRI);
        }

        this.artifactHasEntityIri = getConfiguration().get(Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY);
        if (this.artifactHasEntityIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY);
        }
    }

    @Override
    public void apply(Vertex sourceVertex, final Iterable<Vertex> termMentions, final Authorizations authorizations) throws Exception {
        for (Vertex termMention : termMentions) {
            if (!zipCodeIri.equals(LumifyProperties.CONCEPT_TYPE.getPropertyValue(termMention))) {
                continue;
            }

            String text = LumifyProperties.TITLE.getPropertyValue(termMention);
            if (text.indexOf('-') > 0) {
                text = text.substring(0, text.indexOf('-'));
            }

            ZipCodeEntry zipCodeEntry = zipCodesByZipCode.get(text);
            if (zipCodeEntry == null) {
                continue;
            }

            String id = String.format("GEO-ZIPCODE-%s", zipCodeEntry.getZipCode());
            String sign = String.format("%s - %s, %s", zipCodeEntry.getZipCode(), zipCodeEntry.getCity(), zipCodeEntry.getState());
            GeoPoint geoPoint = new GeoPoint(zipCodeEntry.getLatitude(), zipCodeEntry.getLongitude());
            ElementBuilder<Vertex> resolvedToVertexBuilder = getGraph().prepareVertex(id, sourceVertex.getVisibility())
                    .addPropertyValue(MULTI_VALUE_PROPERTY_KEY, geoLocationIri, geoPoint, sourceVertex.getVisibility());
            LumifyProperties.SOURCE.addPropertyValue(resolvedToVertexBuilder, MULTI_VALUE_PROPERTY_KEY, "Zip Code Resolver", sourceVertex.getVisibility());
            LumifyProperties.TITLE.addPropertyValue(resolvedToVertexBuilder, MULTI_VALUE_PROPERTY_KEY, sign, sourceVertex.getVisibility());
            Vertex zipCodeVertex = resolvedToVertexBuilder.save(authorizations);

            String edgeId = sourceVertex.getId() + "-" + artifactHasEntityIri + "-" + zipCodeVertex.getId();
            Edge resolvedEdge = getGraph().prepareEdge(edgeId, sourceVertex, zipCodeVertex, artifactHasEntityIri, sourceVertex.getVisibility()).save(authorizations);

            new TermMentionBuilder(termMention, zipCodeVertex)
                    .resolvedTo(zipCodeVertex, resolvedEdge)
                    .title(sign)
                    .conceptIri(zipCodeIri)
                    .process(getClass().getName())
                    .visibilitySource(LumifyProperties.VISIBILITY_SOURCE.getPropertyValue(termMention))
                    .save(getGraph(), getVisibilityTranslator(), authorizations);
        }
    }

    private static class ZipCodeEntry {
        private final String zipCode;
        private final String city;
        private final String state;
        private final double latitude;
        private final double longitude;

        private ZipCodeEntry(String zipCode, String city, String state, double latitude, double longitude) {
            this.zipCode = zipCode;
            this.city = city;
            this.state = state;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getZipCode() {
            return zipCode;
        }

        public String getCity() {
            return city;
        }

        public String getState() {
            return state;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}
