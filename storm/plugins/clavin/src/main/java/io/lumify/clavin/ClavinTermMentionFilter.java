package io.lumify.clavin;

import com.bericotech.clavin.extractor.LocationOccurrence;
import com.bericotech.clavin.gazetteer.FeatureClass;
import com.bericotech.clavin.gazetteer.FeatureCode;
import com.bericotech.clavin.gazetteer.GeoName;
import com.bericotech.clavin.resolver.LuceneLocationResolver;
import com.bericotech.clavin.resolver.ResolvedLocation;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.ingest.graphProperty.TermMentionFilterPrepareData;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.ontology.PropertyType;
import io.lumify.core.model.properties.EntityLumifyProperties;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.securegraph.Vertex;
import org.securegraph.type.GeoPoint;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.securegraph.util.IterableUtils.count;

/**
 * This TermResolutionWorker uses the CLAVIN processor to refine
 * identification of location entities.
 */
public class ClavinTermMentionFilter extends TermMentionFilter {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ClavinTermMentionFilter.class);

    private static final String MULTI_VALUE_PROERTY_KEY = ClavinTermMentionFilter.class.getName();

    /**
     * The CLAVIN index directory configuration key.
     */
    public static final String CLAVIN_INDEX_DIRECTORY = "clavin.indexDirectory";

    /**
     * The CLAVIN max hit depth configuration key.
     */
    public static final String CLAVIN_MAX_HIT_DEPTH = "clavin.maxHitDepth";

    /**
     * The CLAVIN max context window configuration key.
     */
    public static final String CLAVIN_MAX_CONTEXT_WINDOW = "clavin.maxContextWindow";

    /**
     * The CLAVIN use fuzzy matching configuration key.
     */
    public static final String CLAVIN_USE_FUZZY_MATCHING = "clavin.useFuzzyMatching";

    /**
     * The default max hit depth.
     */
    public static final int DEFAULT_MAX_HIT_DEPTH = 5;

    /**
     * The default max context window.
     */
    public static final int DEFAULT_MAX_CONTENT_WINDOW = 5;

    /**
     * The default fuzzy matching.
     */
    public static final boolean DEFAULT_FUZZY_MATCHING = false;

    private static final String CONFIG_STATE_IRI = "ontology.iri.state";
    private static final String CONFIG_COUNTRY_IRI = "ontology.iri.country";
    private static final String CONFIG_CITY_IRI = "ontology.iri.city";
    private static final String CONFIG_GEO_LOCATION_IRI = "ontology.iri.geoLocation";
    private static final String CONFIG_EXCLUDED_IRI_PREFIX = "clavin.excludeIri";

    private LuceneLocationResolver resolver;
    private boolean fuzzy;
    private Set<String> targetConcepts;
    private OntologyRepository ontologyRepository;
    private String stateIri;
    private String countryIri;
    private String cityIri;
    private String geoLocationIri;
    private AuditRepository auditRespository;
    private User user;

    @Override
    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
        super.prepare(termMentionFilterPrepareData);

        Configuration config = new Configuration(termMentionFilterPrepareData.getStormConf());

        LOGGER.info("Configuring CLAVIN Location Resolution.");
        prepareIris(termMentionFilterPrepareData);
        prepareClavinLuceneIndex(config);
        prepareFuzzy(config);
        prepareTargetConcepts(config);
        user = termMentionFilterPrepareData.getUser();
    }

    public void prepareTargetConcepts(Configuration config) {
        Set<String> excludedIris = getExcludedIris(config);

        Set<String> conceptsWithGeoLocationProperty = new HashSet<String>();
        for (Concept concept : ontologyRepository.getConceptsWithProperties()) {
            for (OntologyProperty property : concept.getProperties()) {
                String iri = concept.getTitle();
                if (property.getDataType() == PropertyType.GEO_LOCATION && !excludedIris.contains(iri)) {
                    conceptsWithGeoLocationProperty.add(iri);
                    break;
                }
            }
        }
        targetConcepts = Collections.unmodifiableSet(conceptsWithGeoLocationProperty);
    }

    private Set<String> getExcludedIris(Configuration config) {
        Set<String> excludedIris = new HashSet<String>();
        excludedIris.addAll(config.getSubset(CONFIG_EXCLUDED_IRI_PREFIX).values());
        return excludedIris;
    }

    public void prepareFuzzy(Configuration config) {
        String fuzzyStr = config.get(CLAVIN_USE_FUZZY_MATCHING, null);
        if (fuzzyStr != null) {
            fuzzyStr = fuzzyStr.trim();
        }
        if (fuzzyStr != null && Boolean.TRUE.toString().equalsIgnoreCase(fuzzyStr) ||
                Boolean.FALSE.toString().equalsIgnoreCase(fuzzyStr)) {
            fuzzy = Boolean.parseBoolean(fuzzyStr);
            LOGGER.debug("Found %s: %s. fuzzy=%s", CLAVIN_USE_FUZZY_MATCHING, fuzzyStr, fuzzy);
        } else {
            LOGGER.debug("%s not configured. Using default: %s", CLAVIN_USE_FUZZY_MATCHING, DEFAULT_FUZZY_MATCHING);
            fuzzy = DEFAULT_FUZZY_MATCHING;
        }
    }

    public void prepareClavinLuceneIndex(Configuration config) throws IOException, ParseException {
        String idxDirPath = config.get(CLAVIN_INDEX_DIRECTORY, null);
        if (idxDirPath == null || idxDirPath.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("%s must be configured.", CLAVIN_INDEX_DIRECTORY));
        }
        LOGGER.debug("Configuring CLAVIN index [%s]: %s", CLAVIN_INDEX_DIRECTORY, idxDirPath);
        File indexDirectory = new File(idxDirPath);
        if (!indexDirectory.exists() || !indexDirectory.isDirectory()) {
            throw new IllegalArgumentException(String.format("CLAVIN index cannot be found at configured (%s) location: %s",
                    CLAVIN_INDEX_DIRECTORY, idxDirPath));
        }

        int maxHitDepth = config.getInt(CLAVIN_MAX_HIT_DEPTH);
        if (maxHitDepth < 1) {
            LOGGER.debug("Found %s of %d. Using default: %d", CLAVIN_MAX_HIT_DEPTH, maxHitDepth, DEFAULT_MAX_HIT_DEPTH);
            maxHitDepth = DEFAULT_MAX_HIT_DEPTH;
        }
        int maxContextWindow = config.getInt(CLAVIN_MAX_CONTEXT_WINDOW);
        if (maxContextWindow < 1) {
            LOGGER.debug("Found %s of %d. Using default: %d", CLAVIN_MAX_CONTEXT_WINDOW, maxContextWindow, DEFAULT_MAX_CONTENT_WINDOW);
            maxContextWindow = DEFAULT_MAX_CONTENT_WINDOW;
        }

        resolver = new LuceneLocationResolver(indexDirectory, maxHitDepth, maxContextWindow);
    }

    public void prepareIris(TermMentionFilterPrepareData termMentionFilterPrepareData) {
        stateIri = (String) termMentionFilterPrepareData.getStormConf().get(CONFIG_STATE_IRI);
        if (stateIri == null || stateIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_STATE_IRI);
        }

        countryIri = (String) termMentionFilterPrepareData.getStormConf().get(CONFIG_COUNTRY_IRI);
        if (countryIri == null || countryIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_COUNTRY_IRI);
        }

        cityIri = (String) termMentionFilterPrepareData.getStormConf().get(CONFIG_CITY_IRI);
        if (cityIri == null || cityIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_CITY_IRI);
        }

        geoLocationIri = (String) termMentionFilterPrepareData.getStormConf().get(CONFIG_GEO_LOCATION_IRI);
        if (geoLocationIri == null || geoLocationIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_GEO_LOCATION_IRI);
        }
    }

    @Override
    public Iterable<TermMention> apply(Vertex artifactGraphVertex, Iterable<TermMention> termMentions) throws IOException, ParseException {
        List<LocationOccurrence> locationOccurrences = getLocationOccurrencesFromTermMentions(termMentions);
        LOGGER.info("Found %d Locations in %d terms.", locationOccurrences.size(), count(termMentions));
        List<ResolvedLocation> resolvedLocationNames = resolver.resolveLocations(locationOccurrences, fuzzy);
        LOGGER.info("Resolved %d Locations", resolvedLocationNames.size());

        if (resolvedLocationNames.isEmpty()) {
            return termMentions;
        }

        Map<Integer, ResolvedLocation> resolvedLocationOffsetMap = new HashMap<Integer, ResolvedLocation>();
        for (ResolvedLocation resolvedLocation : resolvedLocationNames) {
            // assumes start/end positions are real, i.e., unique start positions for each extracted term
            resolvedLocationOffsetMap.put(resolvedLocation.getLocation().getPosition(), resolvedLocation);
        }

        ResolvedLocation loc;
        String processId = getClass().getName();
        TermMention resolvedMention;
        List<TermMention> results = new ArrayList<TermMention>();
        for (TermMention termMention : termMentions) {
            loc = resolvedLocationOffsetMap.get(termMention.getStart());
            if (isLocation(termMention) && loc != null) {
                String id = String.format("CLAVIN-%d", loc.getGeoname().getGeonameID());
                GeoPoint geoPoint = new GeoPoint(loc.getGeoname().getLatitude(), loc.getGeoname().getLongitude(), termMention.getSign());
                resolvedMention = new TermMention.Builder(termMention)
                        .id(id)
                        .resolved(true)
                        .useExisting(true)
                        .sign(toSign(loc))
                        .ontologyClassUri(getOntologyClassUri(loc, termMention.getOntologyClassUri()))
                        .addProperty(MULTI_VALUE_PROERTY_KEY, geoLocationIri, geoPoint)
                        .addProperty(MULTI_VALUE_PROERTY_KEY, EntityLumifyProperties.SOURCE.getPropertyName(), "CLAVIN")
                        .process(processId)
                        .build();
                LOGGER.debug("Replacing original location [%s] with resolved location [%s]", termMention, resolvedMention);
                results.add(resolvedMention);
            } else {
                results.add(termMention);
            }
        }
        auditRespository.auditAnalyzedBy(AuditAction.ANALYZED_BY, artifactGraphVertex, getClass().getSimpleName(),
                user, artifactGraphVertex.getVisibility());
        return results;
    }

    private String toSign(final ResolvedLocation location) {
        GeoName geoname = location.getGeoname();
        return String.format("%s (%s, %s)", geoname.getName(), geoname.getPrimaryCountryCode(), geoname.getAdmin1Code());
    }

    private boolean isLocation(final TermMention mention) {
        return targetConcepts.contains(mention.getOntologyClassUri());
    }

    private List<LocationOccurrence> getLocationOccurrencesFromTermMentions(final Iterable<TermMention> termMentions) {
        List<LocationOccurrence> locationOccurrences = new ArrayList<LocationOccurrence>();

        for (TermMention termMention : termMentions) {
            if (isLocation(termMention)) {
                locationOccurrences.add(new LocationOccurrence(termMention.getSign(), termMention.getStart()));
            }
        }
        return locationOccurrences;
    }

    public String getOntologyClassUri(final ResolvedLocation location, final String defaultValue) {
        String uri = defaultValue;
        FeatureClass featureClass = location.getGeoname().getFeatureClass();
        FeatureCode featureCode = location.getGeoname().getFeatureCode();
        if (featureClass == null) {
            featureClass = FeatureClass.NULL;
        }
        if (featureCode == null) {
            featureCode = FeatureCode.NULL;
        }
        switch (featureClass) {
            case A:
                switch (featureCode) {
                    case ADM1:
                        uri = stateIri;
                        break;
                    case PCLI:
                        uri = countryIri;
                        break;
                }
                break;
            case P:
                uri = cityIri;
                break;
        }
        return uri;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public void setAuditRepository(AuditRepository auditRepository) {
        this.auditRespository = auditRepository;
    }
}
