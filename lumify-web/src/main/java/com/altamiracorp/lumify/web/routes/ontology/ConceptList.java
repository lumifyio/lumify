package com.altamiracorp.lumify.web.routes.ontology;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.atteo.evo.inflector.English;
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

        JSONObject result = buildJsonTree(request, rootConcept, user);

        respondWithJson(response, result);
        chain.next(request, response);
    }

    private JSONObject buildJsonTree(HttpServletRequest request, Concept concept, User user) throws JSONException {
        JSONObject result = concept.toJson();

        String displayName = result.optString(PropertyName.DISPLAY_NAME.toString());
        if (displayName != null) {
            result.put("pluralDisplayName", English.plural(displayName));
        }

        List<Concept> childConcepts = ontologyRepository.getChildConcepts(concept);
        if (childConcepts.size() > 0) {
            JSONArray childrenJson = new JSONArray();
            for (Concept childConcept : childConcepts) {
                JSONObject childJson = buildJsonTree(request, childConcept, user);
                childrenJson.put(childJson);
            }
            result.put("children", childrenJson);
        }

        return result;
    }
}
