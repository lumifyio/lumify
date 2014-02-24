package com.altamiracorp.lumify.web.routes.ontology;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class ConceptList extends BaseRequestHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public ConceptList(final OntologyRepository repo) {
        ontologyRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);

        Concept rootConcept = ontologyRepository.getRootConcept();

        JSONObject result = buildJsonTree(ontologyRepository, rootConcept);

        respondWithJson(response, result);
        chain.next(request, response);
    }

    public static JSONObject buildJsonTree(OntologyRepository ontologyRepository, Concept concept) throws JSONException {
        JSONObject result = concept.toJson();
        List<Concept> childConcepts = ontologyRepository.getChildConcepts(concept);
        if (childConcepts.size() > 0) {
            JSONArray childrenJson = new JSONArray();
            for (Concept childConcept : childConcepts) {
                JSONObject childJson = buildJsonTree(ontologyRepository, childConcept);
                childrenJson.put(childJson);
            }
            result.put("children", childrenJson);
        }

        return result;
    }
}
