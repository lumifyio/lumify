package io.lumify.web.routes.graph;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.detectedObjects.DetectedObjectRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.ontology.PropertyType;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.query.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.List;

import static io.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;

public class GraphVertexSearch extends BaseRequestHandler {
    private final int DEFAULT_RESULT_COUNT = 100;

    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphVertexSearch.class);
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final UserRepository userRepository;

    @Inject
    public GraphVertexSearch(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            final DetectedObjectRepository detectedObjectRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.detectedObjectRepository = detectedObjectRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String query;
        final String filter = getRequiredParameter(request, "filter");
        final int offset = (int) getOptionalParameterLong(request, "offset", 0);
        final int size = (int) getOptionalParameterLong(request, "size", DEFAULT_RESULT_COUNT);
        final String conceptType = getOptionalParameter(request, "conceptType");
        final String getLeafNodes = getOptionalParameter(request, "leafNodes");
        final String relatedToVertexId = getOptionalParameter(request, "relatedToVertexId");
        if (relatedToVertexId == null) {
            query = getRequiredParameter(request, "q");
        } else {
            query = getOptionalParameter(request, "q");
        }

        long startTime = System.nanoTime();

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);
        ModelUserContext modelUserContext = userRepository.getModelUserContext(authorizations, workspaceId);

        JSONArray filterJson = new JSONArray(filter);

        ontologyRepository.resolvePropertyIds(filterJson);

        graph.flush();

        LOGGER.debug("search %s\n%s", query, filterJson.toString(2));

        Query graphQuery;
        if (relatedToVertexId == null) {
            graphQuery = graph.query(query, authorizations);
        } else if (query == null || StringUtils.isBlank(query)) {
            graphQuery = graph.getVertex(relatedToVertexId, authorizations).query(authorizations);
        } else {
            graphQuery = graph.getVertex(relatedToVertexId, authorizations).query(query, authorizations);
        }

        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject obj = filterJson.getJSONObject(i);
            if (obj.length() > 0) {
                updateQueryWithFilter(graphQuery, obj);
            }
        }

        if (conceptType != null) {
            Concept concept = ontologyRepository.getConceptByIRI(conceptType);
            if (getLeafNodes == null || !getLeafNodes.equals("false")) {
                List<Concept> leafNodeList = ontologyRepository.getAllLeafNodesByConcept(concept);
                if (leafNodeList.size() > 0) {
                    String[] conceptIds = new String[leafNodeList.size()];
                    int count = 0;
                    for (Concept c : leafNodeList) {
                        conceptIds[count] = c.getTitle();
                        count++;
                    }
                    graphQuery.has(CONCEPT_TYPE.getKey(), Compare.IN, conceptIds);
                }
            } else {
                graphQuery.has(CONCEPT_TYPE.getKey(), conceptType);
            }
        }

        graphQuery.limit(size);
        graphQuery.skip(offset);
        Iterable<Vertex> searchResults;
        try {
            searchResults = graphQuery.vertices();
        } catch (SearchPhaseExecutionException ex) {
            respondWithBadRequest(response, "q", "Invalid Query");
            return;
        }

        JSONArray verticesJson = new JSONArray();
        int verticesCount = 0;
        for (Vertex vertex : searchResults) {
            verticesJson.put(JsonSerializer.toJson(vertex, workspaceId));
            verticesJson.getJSONObject(verticesCount).put("detectedObjects", detectedObjectRepository.toJSON(vertex, modelUserContext, authorizations, workspaceId));
            verticesCount++;
        }

        JSONObject results = new JSONObject();
        results.put("vertices", verticesJson);
        results.put("nextOffset", offset + size);

        if (searchResults instanceof IterableWithTotalHits) {
            IterableWithTotalHits searchResultsFaceted = (IterableWithTotalHits) searchResults;
            if (searchResultsFaceted.getTotalHits() != null) {
                results.put("totalHits", searchResultsFaceted.getTotalHits());
            }
        }

        long endTime = System.nanoTime();
        LOGGER.info("Search for \"%s\" found %d vertices in %dms", query, verticesCount, (endTime - startTime) / 1000 / 1000);

        respondWithJson(response, results);
    }

    private void updateQueryWithFilter(Query graphQuery, JSONObject obj) throws ParseException {
        String predicateString = obj.optString("predicate");
        JSONArray values = obj.getJSONArray("values");
        PropertyType propertyDataType = PropertyType.convert(obj.optString("propertyDataType"));
        String propertyName = obj.getString("propertyName");
        Object value0 = jsonValueToObject(values, propertyDataType, 0);

        if (PropertyType.STRING.equals(propertyDataType) && (predicateString == null || "".equals(predicateString))) {
            graphQuery.has(propertyName, TextPredicate.CONTAINS, value0);
        } else if (PropertyType.BOOLEAN.equals(propertyDataType) && (predicateString == null || "".equals(predicateString))) {
            graphQuery.has(propertyName, Compare.EQUAL, value0);
        } else if ("<".equals(predicateString)) {
            graphQuery.has(propertyName, Compare.LESS_THAN, value0);
        } else if (">".equals(predicateString)) {
            graphQuery.has(propertyName, Compare.GREATER_THAN, value0);
        } else if ("range".equals(predicateString)) {
            graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, value0);
            graphQuery.has(propertyName, Compare.LESS_THAN_EQUAL, jsonValueToObject(values, propertyDataType, 1));
        } else if ("=".equals(predicateString) || "equal".equals(predicateString)) {
            graphQuery.has(propertyName, Compare.EQUAL, value0);
        } else if (PropertyType.GEO_LOCATION.equals(propertyDataType)) {
            graphQuery.has(propertyName, GeoCompare.WITHIN, value0);
        } else {
            throw new LumifyException("unhandled query\n" + obj.toString(2));
        }
    }

    private Object jsonValueToObject(JSONArray values, PropertyType propertyDataType, int index) throws ParseException {
        return OntologyProperty.convert(values, propertyDataType, index);
    }
}
