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
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.PropertyType;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.apache.lucene.queryparser.classic.ParseException;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.ElementBuilder;
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

    public static final String MULTI_VALUE_PROPERTY_KEY = ClavinTermMentionFilter.class.getName();

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
    private AuditRepository auditRepository;
    private User user;
    private String artifactHasEntityIri;
    private WorkspaceRepository workspaceRepository;

    @Override
    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
        super.prepare(termMentionFilterPrepareData);

        LOGGER.info("Configuring CLAVIN Location Resolution.");
        prepareIris(termMentionFilterPrepareData);
        prepareClavinLuceneIndex(getConfiguration());
        prepareFuzzy(getConfiguration());
        prepareTargetConcepts(getConfiguration());
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
        this.artifactHasEntityIri = getConfiguration().get(Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY, null);
        if (this.artifactHasEntityIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY);
        }

        stateIri = (String) termMentionFilterPrepareData.getConfiguration().get(CONFIG_STATE_IRI);
        if (stateIri == null || stateIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_STATE_IRI);
        }

        countryIri = (String) termMentionFilterPrepareData.getConfiguration().get(CONFIG_COUNTRY_IRI);
        if (countryIri == null || countryIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_COUNTRY_IRI);
        }

        cityIri = (String) termMentionFilterPrepareData.getConfiguration().get(CONFIG_CITY_IRI);
        if (cityIri == null || cityIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_CITY_IRI);
        }

        geoLocationIri = (String) termMentionFilterPrepareData.getConfiguration().get(CONFIG_GEO_LOCATION_IRI);
        if (geoLocationIri == null || geoLocationIri.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_GEO_LOCATION_IRI);
        }
    }

    @Override
    public void apply(Vertex sourceVertex, Iterable<Vertex> termMentions, Authorizations authorizations) throws IOException, ParseException {
        List<LocationOccurrence> locationOccurrences = getLocationOccurrencesFromTermMentions(termMentions);
        LOGGER.info("Found %d Locations in %d terms.", locationOccurrences.size(), count(termMentions));
        List<ResolvedLocation> resolvedLocationNames = resolver.resolveLocations(locationOccurrences, fuzzy);
        LOGGER.info("Resolved %d Locations", resolvedLocationNames.size());

        if (resolvedLocationNames.isEmpty()) {
            return;
        }

        Map<Integer, ResolvedLocation> resolvedLocationOffsetMap = new HashMap<Integer, ResolvedLocation>();
        for (ResolvedLocation resolvedLocation : resolvedLocationNames) {
            // assumes start/end positions are real, i.e., unique start positions for each extracted term
            resolvedLocationOffsetMap.put(resolvedLocation.getLocation().getPosition(), resolvedLocation);
        }

        ResolvedLocation loc;
        String processId = getClass().getName();
        for (Vertex termMention : termMentions) {
            loc = resolvedLocationOffsetMap.get((int) LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(termMention, 0));
            if (isLocation(termMention) && loc != null) {
                String id = String.format("CLAVIN-%d", loc.getGeoname().getGeonameID());
                GeoPoint geoPoint = new GeoPoint(loc.getGeoname().getLatitude(), loc.getGeoname().getLongitude(), LumifyProperties.TERM_MENTION_TITLE.getPropertyValue(termMention));
                String title = toSign(loc);
                String termMentionConceptType = LumifyProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyValue(termMention);
                String conceptType = getOntologyClassUri(loc, termMentionConceptType);

                VisibilityJson sourceVertexVisibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(sourceVertex);
                Map<String, Object> metadata = new HashMap<String, Object>();
                LumifyProperties.VISIBILITY_JSON.setMetadata(metadata, sourceVertexVisibilityJson);
                ElementBuilder<Vertex> resolvedToVertexBuilder = getGraph().prepareVertex(id, sourceVertex.getVisibility())
                        .addPropertyValue(MULTI_VALUE_PROPERTY_KEY, geoLocationIri, geoPoint, metadata, sourceVertex.getVisibility());
                LumifyProperties.CONCEPT_TYPE.addPropertyValue(resolvedToVertexBuilder, MULTI_VALUE_PROPERTY_KEY, conceptType, metadata, sourceVertex.getVisibility());
                LumifyProperties.SOURCE.addPropertyValue(resolvedToVertexBuilder, MULTI_VALUE_PROPERTY_KEY, "CLAVIN", metadata, sourceVertex.getVisibility());
                LumifyProperties.TITLE.addPropertyValue(resolvedToVertexBuilder, MULTI_VALUE_PROPERTY_KEY, title, metadata, sourceVertex.getVisibility());
                LumifyProperties.VISIBILITY_JSON.addPropertyValue(resolvedToVertexBuilder, MULTI_VALUE_PROPERTY_KEY, sourceVertexVisibilityJson, metadata, sourceVertex.getVisibility());
                Vertex resolvedToVertex = resolvedToVertexBuilder.save(authorizations);
                getGraph().flush();

                String edgeId = sourceVertex.getId() + "-" + artifactHasEntityIri + "-" + resolvedToVertex.getId();
                Edge resolvedEdge = getGraph().prepareEdge(edgeId, sourceVertex, resolvedToVertex, artifactHasEntityIri, sourceVertex.getVisibility()).save(authorizations);
                LumifyProperties.VISIBILITY_JSON.addPropertyValue(resolvedEdge, MULTI_VALUE_PROPERTY_KEY, sourceVertexVisibilityJson, metadata, sourceVertex.getVisibility(), authorizations);
                VisibilityJson visibilityJson = LumifyProperties.TERM_MENTION_VISIBILITY_JSON.getPropertyValue(termMention);
                if (visibilityJson != null && visibilityJson.getWorkspaces().size() > 0) {
                    Set<String> workspaceIds = visibilityJson.getWorkspaces();
                    for (String workspaceId : workspaceIds) {
                        workspaceRepository.updateEntityOnWorkspace(workspaceRepository.findById(workspaceId, user), id, false, null, user);
                    }
                }

                Vertex resolvedMention = new TermMentionBuilder(termMention, sourceVertex)
                        .resolvedTo(resolvedToVertex, resolvedEdge)
                        .title(title)
                        .conceptIri(conceptType)
                        .process(processId)
                        .visibilityJson(LumifyProperties.TERM_MENTION_VISIBILITY_JSON.getPropertyValue(termMention))
                        .save(getGraph(), getVisibilityTranslator(), authorizations);

                LOGGER.debug("Replacing original location [%s] with resolved location [%s]", termMention.getId(), resolvedMention.getId());
            }
        }
        auditRepository.auditAnalyzedBy(AuditAction.ANALYZED_BY, sourceVertex, getClass().getSimpleName(), user, sourceVertex.getVisibility());
    }

    private String toSign(final ResolvedLocation location) {
        GeoName geoname = location.getGeoname();
        return String.format("%s (%s, %s)", geoname.getName(), geoname.getPrimaryCountryCode(), geoname.getAdmin1Code());
    }

    private boolean isLocation(final Vertex mention) {
        return targetConcepts.contains(LumifyProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyValue(mention));
    }

    private List<LocationOccurrence> getLocationOccurrencesFromTermMentions(final Iterable<Vertex> termMentions) {
        List<LocationOccurrence> locationOccurrences = new ArrayList<LocationOccurrence>();

        for (Vertex termMention : termMentions) {
            if (isLocation(termMention)) {
                locationOccurrences.add(new LocationOccurrence(LumifyProperties.TERM_MENTION_TITLE.getPropertyValue(termMention), (int) LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(termMention, 0)));
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
        this.auditRepository = auditRepository;
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }
}
