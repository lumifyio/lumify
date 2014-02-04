package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.query.Compare;
import com.altamiracorp.securegraph.query.Predicate;
import com.altamiracorp.securegraph.query.Query;
import com.altamiracorp.securegraph.query.TextPredicate;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.altamiracorp.lumify.core.util.GraphUtil.toJson;

public class GraphVertexSearch extends BaseRequestHandler {
    //TODO should we limit to 10000??
    private final int MAX_RESULT_COUNT = 10000;

    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphVertexSearch.class);
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public GraphVertexSearch(final OntologyRepository ontologyRepo, final Graph graph) {
        ontologyRepository = ontologyRepo;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String query;
        final String filter = getRequiredParameter(request, "filter");
        final long offset = getOptionalParameterLong(request, "offset", 0);
        final long size = getOptionalParameterLong(request, "size", 100);
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
        JSONArray filterJson = new JSONArray(filter);

        ontologyRepository.resolvePropertyIds(filterJson);

        graph.flush();

        Query graphQuery = null;
        if (relatedToVertexId == null) {
            graphQuery = graph.query(query, user.getAuthorizations());
        } else if (query == null || StringUtils.isBlank(query)) {
            graphQuery = graph.getVertex(relatedToVertexId, user.getAuthorizations()).query(user.getAuthorizations());
        } else {
            graphQuery = graph.getVertex(relatedToVertexId, user.getAuthorizations()).query(query, user.getAuthorizations());
        }

        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject obj = filterJson.getJSONObject(i);
            if (obj.length() > 0) {
                if (obj.getJSONArray("values").length() > 0) {
                    JSONArray values = obj.getJSONArray("values");
                    for (int j = 0; j < values.length(); j++) {
                        Object val = values.get(j);
                        Predicate predicate = Compare.EQUAL;
                        // TODO how can we specify a contains query?
                        graphQuery.has(obj.getString("propertyName"), predicate, val);
                    }
                }
            }
        }

        if (conceptType != null) {
            Concept concept = ontologyRepository.getConceptById(conceptType);
            if (getLeafNodes == null || !getLeafNodes.equals("false")) {
                List<Concept> leafNodeList = ontologyRepository.getAllLeafNodesByConcept(concept);
                if (leafNodeList.size() > 0) {
                    String[] conceptIds = new String[leafNodeList.size()];
                    int count = 0;
                    for (Concept c : leafNodeList) {
                        conceptIds[count] = (String) c.getId();
                        count++;
                    }
                    graphQuery.has(PropertyName.CONCEPT_TYPE.toString(), Compare.IN, conceptIds);
                }
            } else {
                graphQuery.has(PropertyName.CONCEPT_TYPE.toString(), conceptType);
            }
        }

        graphQuery.limit(MAX_RESULT_COUNT);
        Iterable<Vertex> searchResults = graphQuery.vertices();

        JSONArray vertices = new JSONArray();
        JSONObject counts = new JSONObject();
        int verticesCount = 0;
        for (Vertex vertex : searchResults) {
            if (verticesCount >= offset && verticesCount <= offset + size) {
                vertices.put(toJson(vertex));
            }
            Object conceptyType = vertex.getPropertyValue(PropertyName.CONCEPT_TYPE.toString(), 0);
            String type;
            if (conceptyType == null) {
                type = "Unknown";
            } else {
                type = conceptyType.toString();
            }
            if (counts.keySet().contains(type)) {
                counts.put(type, (counts.getInt(type) + 1));
            } else {
                counts.put(type, 1);
            }
            verticesCount++;
            // TODO this used create hierarchical results
        }

        JSONObject results = new JSONObject();
        results.put("vertices", vertices);
        results.put("verticesCount", counts);

        long endTime = System.nanoTime();
        LOGGER.info("Search for \"%s\" found %d vertices in %dms", query, verticesCount, (endTime - startTime) / 1000 / 1000);

        respondWithJson(response, results);
    }
}
