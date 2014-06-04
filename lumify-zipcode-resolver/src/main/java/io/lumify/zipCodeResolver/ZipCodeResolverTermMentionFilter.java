package io.lumify.zipCodeResolver;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.ingest.graphProperty.TermMentionFilterPrepareData;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.model.properties.EntityLumifyProperties;
import org.securegraph.Vertex;
import org.securegraph.type.GeoPoint;
import org.securegraph.util.ConvertingIterable;
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
    }

    @Override
    public Iterable<TermMention> apply(Vertex artifactGraphVertex, final Iterable<TermMention> termMentions) throws Exception {
        return new ConvertingIterable<TermMention, TermMention>(termMentions) {
            @Override
            protected TermMention convert(TermMention termMention) {
                if (!zipCodeIri.equals(termMention.getOntologyClassUri())) {
                    return termMention;
                }

                String text = termMention.getSign();
                if (text.indexOf('-') > 0) {
                    text = text.substring(0, text.indexOf('-'));
                }

                ZipCodeEntry zipCodeEntry = zipCodesByZipCode.get(text);
                if (zipCodeEntry == null) {
                    return termMention;
                }

                String id = String.format("GEO-ZIPCODE-%s", zipCodeEntry.getZipCode());
                String sign = String.format("%s - %s, %s", zipCodeEntry.getZipCode(), zipCodeEntry.getCity(), zipCodeEntry.getState());
                GeoPoint geoPoint = new GeoPoint(zipCodeEntry.getLatitude(), zipCodeEntry.getLongitude());
                return new TermMention.Builder(termMention)
                        .id(id)
                        .resolved(true)
                        .useExisting(true)
                        .sign(sign)
                        .ontologyClassUri(zipCodeIri)
                        .addProperty(MULTI_VALUE_PROPERTY_KEY, geoLocationIri, geoPoint)
                        .addProperty(MULTI_VALUE_PROPERTY_KEY, EntityLumifyProperties.SOURCE.getKey(), "Zip Code Resolver")
                        .process(getClass().getName())
                        .build();
            }
        };
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
