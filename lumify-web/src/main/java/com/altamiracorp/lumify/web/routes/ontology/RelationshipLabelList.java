package com.altamiracorp.lumify.web.routes.ontology;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Relationship;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class RelationshipLabelList extends BaseRequestHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public RelationshipLabelList(final OntologyRepository repo) {
        ontologyRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String sourceConceptTypeId = getOptionalParameter(request, "sourceConceptTypeId");
        final String destConceptTypeId = getOptionalParameter(request, "destConceptTypeId");

        if (destConceptTypeId == null) {
            Iterable<Relationship> relationships = ontologyRepository.getRelationshipLabels();

            JSONObject json = new JSONObject();
            json.put("relationships", Relationship.toJsonRelationships(relationships));

            respondWithJson(response, json);
        } else {
            Iterable<Relationship> relationships = ontologyRepository.getRelationships(sourceConceptTypeId, destConceptTypeId);

            JSONObject result = new JSONObject();
            JSONArray relationshipsJson = Relationship.toJsonRelationships(relationships);
            result.put("relationships", relationshipsJson);

            respondWithJson(response, result);
        }
    }
}
