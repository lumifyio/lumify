package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.model.graph.GraphPagedResults;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public class GraphVertexSearch extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphVertexSearch.class);
    private final GraphRepository graphRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public GraphVertexSearch(final OntologyRepository ontologyRepo, final GraphRepository graphRepo) {
        ontologyRepository = ontologyRepo;
        graphRepository = graphRepo;
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

        graphRepository.commit();

        GraphPagedResults pagedResults = graphRepository.search(query, filterJson, user, offset, size != 0 && size != offset ? size - 1 : size, conceptType);

        JSONArray vertices = new JSONArray();
        JSONObject counts = new JSONObject();
        int verticesCount = 0;
        for (Map.Entry<String, List<GraphVertex>> entry : pagedResults.getResults().entrySet()) {
            JSONArray temp = GraphVertex.toJson(entry.getValue());
            for (int i = 0; i < temp.length(); i++) {
                vertices.put(temp.getJSONObject(i));
            }
            Integer count = pagedResults.getCount().get(entry.getKey());
            verticesCount += count.intValue();
            counts.put(entry.getKey(), count);
        }
        LOGGER.info("Number of vertices returned for query: %d", verticesCount);

        JSONObject results = new JSONObject();
        results.put("vertices", vertices);
        results.put("verticesCount", counts);

        respondWithJson(response, results);
    }
}
