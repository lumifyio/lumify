package com.altamiracorp.lumify.search;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.metrics.MetricsManager;
import com.altamiracorp.lumify.core.model.artifact.ArtifactType;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.search.ArtifactSearchPagedResults;
import com.altamiracorp.lumify.core.model.search.ArtifactSearchResult;
import com.altamiracorp.lumify.core.model.search.SearchProvider;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.codahale.metrics.Timer;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class ElasticSearchProvider extends SearchProvider {
    public static final String ES_LOCATIONS_PROP_KEY = "search.elasticsearch.locations";

    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ElasticSearchProvider.class);
    private static final String ES_INDEX = "atc";
    private static final String ES_INDEX_TYPE = "artifact";
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_SUBJECT = "subject";
    private static final String FIELD_PUBLISHED_DATE = "publishedDate";
    private static final String FIELD_GRAPH_VERTEX_ID = "graphVertexId";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_CONCEPT_TYPE = "conceptType";
    private static final String FIELD_GEO_LOCATION_DESCRIPTION = "geoLocationDescription";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final int ES_QUERY_MAX_SIZE = 100;

    private static TransportClient client;
    private Timer processingTimeTimer;

    @Override
    public void init(Configuration config, User user, MetricsManager metricsManager) {
        String namePrefix = metricsManager.getNamePrefix(this);
        processingTimeTimer = metricsManager.getRegistry().timer(namePrefix + "processing-time");

        String[] esLocations = config.get(ES_LOCATIONS_PROP_KEY).split(",");
        setup(esLocations, user);
    }

    private void setup(String[] esLocations, User user) {
        if (client != null) {
            return;
        }

        client = new TransportClient();
        for (String esLocation : esLocations) {
            String[] locationSocket = esLocation.split(":");
            client.addTransportAddress(new InetSocketTransportAddress(locationSocket[0], Integer.parseInt(locationSocket[1])));
        }

        initializeIndex(user);
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public void add(GraphVertex graphVertex, InputStream textIn) throws Exception {
        checkNotNull(graphVertex);
        checkNotNull(textIn);

        LOGGER.info("Adding data from graph vertex (id: %s) to elastic search index", graphVertex.getId());

        String id = (String) graphVertex.getProperty(PropertyName.ROW_KEY);
        String graphVertexId = graphVertex.getId();
        String source = (String) graphVertex.getProperty(PropertyName.SOURCE);
        String geoLocationDescription = (String) graphVertex.getProperty(PropertyName.GEO_LOCATION_DESCRIPTION);
        String text = IOUtils.toString(textIn); // TODO if the text is really large this is going to eat all the memory
        text = text == null ? "" : text;
        String subject = (String) graphVertex.getProperty(PropertyName.TITLE);
        subject = subject == null ? "" : subject;
        String publishedDate = graphVertex.getProperty(PropertyName.PUBLISHED_DATE) == null ? null : Long.toString((Long) graphVertex.getProperty(PropertyName.PUBLISHED_DATE));

        XContentBuilder jsonBuilder = XContentFactory.jsonBuilder()
                .startObject()
                .field(FIELD_TEXT, text)
                .field(FIELD_SUBJECT, subject)
                .field(FIELD_CONCEPT_TYPE, graphVertex.getProperty(PropertyName.CONCEPT_TYPE))
                .field(FIELD_GRAPH_VERTEX_ID, graphVertexId);

        if (publishedDate != null) {
            jsonBuilder = jsonBuilder.field(FIELD_PUBLISHED_DATE, publishedDate);
        }

        if (source != null) {
            jsonBuilder = jsonBuilder.field(FIELD_SOURCE, source);
        }

        if (geoLocationDescription != null) {
            jsonBuilder = jsonBuilder.field(FIELD_GEO_LOCATION_DESCRIPTION, geoLocationDescription);
        }

        Timer.Context processingTimeTimerContext = processingTimeTimer.time();
        try {
            IndexResponse response = client.prepareIndex(ES_INDEX, ES_INDEX_TYPE, id)
                    .setSource(jsonBuilder.endObject())
                    .execute().actionGet();

            if (response.getId() == null) {
                LOGGER.error("Failed to index artifact %s with elastic search", id);
            }
        } finally {
            processingTimeTimerContext.stop();
        }
    }

    @Override
    public Collection<ArtifactSearchResult> searchArtifacts(String query, User user) throws Exception {
        Map<String, Collection<ArtifactSearchResult>> results = searchArtifacts(query, user, 0, ES_QUERY_MAX_SIZE, null).getResults();

        List<ArtifactSearchResult> searchResults = new ArrayList<ArtifactSearchResult>();
        for (Map.Entry<String, Collection<ArtifactSearchResult>> entry : results.entrySet()) {
            searchResults.addAll(entry.getValue());
        }

        return searchResults;
    }

    @Override
    public ArtifactSearchPagedResults searchArtifacts(String query, User user, int offset, int size, String subType) throws Exception {

        SearchRequestBuilder requestBuilder = client.prepareSearch(ES_INDEX)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes(ES_INDEX_TYPE)
                .setQuery(new QueryStringQueryBuilder(query).defaultField("_all"))
                .setFrom(offset)
                .setSize(size)
                .addFacet(FacetBuilders.termsFacet(FIELD_CONCEPT_TYPE).field(FIELD_CONCEPT_TYPE))
                .addFields(FIELD_SUBJECT, FIELD_GRAPH_VERTEX_ID, FIELD_SOURCE, FIELD_PUBLISHED_DATE, FIELD_CONCEPT_TYPE);

        if (subType != null) {
            requestBuilder.setFilter(FilterBuilders.inFilter(FIELD_CONCEPT_TYPE, subType));
        }

        SearchResponse response = requestBuilder.execute().actionGet();
        SearchHit[] hits = response.getHits().getHits();

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        ArtifactSearchPagedResults pagedResults = new ArtifactSearchPagedResults();

        TermsFacet facet = response.getFacets().facet(FIELD_CONCEPT_TYPE);
        for (TermsFacet.Entry entry : facet) {
            String term = entry.getTerm().toString();

            if (!pagedResults.getResults().containsKey(term)) {
                pagedResults.getResults().put(term, new ArrayList<ArtifactSearchResult>(entry.getCount()));
            }

            pagedResults.getCount().put(term, entry.getCount());
        }

        for (SearchHit hit : hits) {
            Map<String, SearchHitField> fields = hit.getFields();
            String id = hit.getId();
            String subject = getString(fields, FIELD_SUBJECT);
            String source = getString(fields, FIELD_SOURCE);
            String graphVertexId = getString(fields, FIELD_GRAPH_VERTEX_ID);
            ArtifactType type = ArtifactType.convert(getString(fields, FIELD_CONCEPT_TYPE));

            Date publishedDate = new Date();
            String publishedDateString = getString(fields, FIELD_PUBLISHED_DATE);
            if (publishedDateString != null) {
                publishedDate = dateFormat.parse(publishedDateString);
            }

            ArtifactSearchResult result = new ArtifactSearchResult(id, subject, publishedDate, source, type, graphVertexId);

            pagedResults.getResults().get(getString(fields, FIELD_CONCEPT_TYPE)).add(result);
        }


        return pagedResults;
    }

    @Override
    public void deleteIndex(User user) {
        DeleteIndexResponse response = client.admin().indices().delete(new DeleteIndexRequest(ES_INDEX)).actionGet();
        if (!response.isAcknowledged()) {
            LOGGER.error("Failed to delete elastic search index named %s", ES_INDEX);
        }
    }

    @Override
    public void initializeIndex(User user) {
        try {
            IndicesExistsResponse existsResponse = client.admin().indices().exists(new IndicesExistsRequest(ES_INDEX)).actionGet();
            if (existsResponse.isExists()) {
                LOGGER.info("Elastic search index %s already exists, skipping creation.", ES_INDEX);
                return;
            }

            JSONObject indexConfig = new JSONObject();
            indexConfig.put("_source", new JSONObject().put("enabled", false));
            JSONObject properties = new JSONObject();
            properties.put(FIELD_TEXT, new JSONObject().put("type", "string").put("store", "no"));
            properties.put(FIELD_SUBJECT, new JSONObject().put("type", "string").put("store", "yes"));
            properties.put(FIELD_GRAPH_VERTEX_ID, new JSONObject().put("type", "string").put("store", "yes"));
            properties.put(FIELD_SOURCE, new JSONObject().put("type", "string").put("store", "yes"));
            properties.put(FIELD_CONCEPT_TYPE, new JSONObject().put("type", "string").put("store", "yes"));
            properties.put(FIELD_PUBLISHED_DATE, new JSONObject().put("type", "date").put("store", "yes"));
            properties.put(FIELD_GEO_LOCATION_DESCRIPTION, new JSONObject().put("type", "string").put("store", "no"));
            indexConfig.put("properties", properties);
            JSONObject indexType = new JSONObject();
            indexType.put(ES_INDEX_TYPE, indexConfig);

            CreateIndexRequest request = new CreateIndexRequest(ES_INDEX).mapping(ES_INDEX_TYPE, indexType.toString());
            CreateIndexResponse response = client.admin().indices().create(request).actionGet();

            if (!response.isAcknowledged()) {
                LOGGER.error("Failed to create elastic search index named %s", ES_INDEX);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to create Elastic Search index named " + ES_INDEX, e);
        }
    }

    private String getString(Map<String, SearchHitField> fields, String fieldName) {
        SearchHitField field = fields.get(fieldName);
        if (field != null) {
            Object value = field.getValue();
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }
}
