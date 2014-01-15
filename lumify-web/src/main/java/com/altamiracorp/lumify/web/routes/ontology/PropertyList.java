package com.altamiracorp.lumify.web.routes.ontology;

import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class PropertyList extends BaseRequestHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public PropertyList(final OntologyRepository repo) {
        ontologyRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);

        List<OntologyProperty> properties = ontologyRepository.getProperties(user);

        JSONObject json = new JSONObject();
        json.put("properties", OntologyProperty.toJsonProperties(properties));

        respondWithJson(response, json);
    }
}
