package com.altamiracorp.lumify.web.routes.ontology;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Relationship;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class Ontology extends BaseRequestHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public Ontology(
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        JSONObject resultJson = new JSONObject();

        Iterable<Concept> concepts = ontologyRepository.getConceptsWithProperties();
        resultJson.put("concepts", Concept.toJsonConcepts(concepts));

        List<OntologyProperty> properties = ontologyRepository.getProperties();
        resultJson.put("properties", OntologyProperty.toJsonProperties(properties));

        Iterable<Relationship> relationships = ontologyRepository.getRelationshipLabels();
        resultJson.put("relationships", Relationship.toJsonRelationships(relationships));

        respondWithJson(response, resultJson);
    }
}
