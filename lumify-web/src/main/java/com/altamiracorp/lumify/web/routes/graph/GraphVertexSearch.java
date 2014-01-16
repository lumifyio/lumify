package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static com.altamiracorp.lumify.core.util.GraphUtil.toJson;

public class GraphVertexSearch extends BaseRequestHandler {
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
        final String query = getRequiredParameter(request, "q");
        final String filter = getRequiredParameter(request, "filter");
        final long offset = getOptionalParameterLong(request, "offset", 0);
        final long size = getOptionalParameterLong(request, "size", 100);
        final String conceptType = getOptionalParameter(request, "conceptType");

        User user = getUser(request);
        JSONArray filterJson = new JSONArray(filter);

        ontologyRepository.resolvePropertyIds(filterJson, user);

        graph.flush();

        // TODO user the filterJson   OLD CODE: .search(query, filterJson, user, offset, size != 0 && size != offset ? size - 1 : size, conceptType);
        // TODO page results
        Iterable<Vertex> searchResults = graph.query(query, user.getAuthorizations())
                .vertices();

        JSONArray vertices = new JSONArray();
        JSONObject counts = new JSONObject();
        int verticesCount = 0;
        for (Vertex vertex : searchResults) {
            vertices.put(toJson(vertex));
            String type = vertex.getPropertyValue(PropertyName.CONCEPT_TYPE.toString(), 0).toString();
            if (counts.keySet().contains(type)){
                counts.put(type, (counts.getInt(type) + 1));
            } else {
                counts.put(type, 1);
            }
             verticesCount++;
            // TODO this used create hierarchical results
        }
        LOGGER.info("Number of vertices returned for query: %d", verticesCount);

        JSONObject results = new JSONObject();
        results.put("vertices", vertices);
        results.put("verticesCount", counts);

        respondWithJson(response, results);
    }
}
