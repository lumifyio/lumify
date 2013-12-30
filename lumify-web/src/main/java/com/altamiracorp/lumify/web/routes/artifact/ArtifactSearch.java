package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.graph.GraphPagedResults;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public class ArtifactSearch extends BaseRequestHandler {
    private final ArtifactRepository artifactRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public ArtifactSearch(
            ArtifactRepository artifactRepository,
            OntologyRepository ontologyRepository) {
        this.artifactRepository = artifactRepository;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String query = getRequiredParameter(request, "q");
        final String filter = getRequiredParameter(request, "filter");
        final int offset = (int) getOptionalParameterLong(request, "offset", 0);
        final int size = (int) getOptionalParameterLong(request, "size", 100);
        final String type = getOptionalParameter(request, "subType");

        User user = getUser(request);
        JSONArray filterJson = new JSONArray(filter);

        ontologyRepository.resolvePropertyIds(filterJson, user);

        GraphPagedResults pagedResults = artifactRepository.search(query, filterJson, user, offset, size, type);

        JSONObject results = new JSONObject();
        JSONObject counts = new JSONObject();
        for (Map.Entry<String, List<GraphVertex>> entry : pagedResults.getResults().entrySet()) {
            results.put(entry.getKey(), GraphVertex.toJson(entry.getValue()));
            counts.put(entry.getKey(), pagedResults.getCount().get(entry.getKey()));
        }
        results.put("counts", counts);

        respondWithJson(response, results);
    }
}
