package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class VertexToVertexRelationship extends BaseRequestHandler {
    private final GraphRepository graphRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexToVertexRelationship(final OntologyRepository ontologyRepo, final GraphRepository graphRepo) {
        ontologyRepository = ontologyRepo;
        graphRepository = graphRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String source = getRequiredParameter(request, "source");
        final String target = getRequiredParameter(request, "target");
        final String label = getRequiredParameter(request, "label");

        User user = getUser(request);
        Map<String, String> properties = graphRepository.getEdgeProperties(source, target, label, user);
        GraphVertex sourceVertex = graphRepository.findVertex(source, user);
        GraphVertex targetVertex = graphRepository.findVertex(target, user);

        JSONObject results = new JSONObject();
        results.put("source", resultsToJson(source, sourceVertex));
        results.put("target", resultsToJson(target, targetVertex));

        JSONArray propertyJson = new JSONArray();
        for (Map.Entry<String, String> p : properties.entrySet()) {
            JSONObject property = new JSONObject();
            property.put("key", p.getKey());
            String displayName = ontologyRepository.getDisplayNameForLabel(p.getValue(), user);
            if (displayName == null) {
                property.put("value", p.getValue());
            } else {
                property.put("value", displayName);
            }
            propertyJson.put(property);
        }
        results.put("properties", propertyJson);

        respondWithJson(response, results);
    }

    private JSONObject resultsToJson(String id, GraphVertex graphVertex) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        for (String property : graphVertex.getPropertyKeys()) {
            json.put(property, graphVertex.getProperty(property));
        }

        return json;
    }
}
