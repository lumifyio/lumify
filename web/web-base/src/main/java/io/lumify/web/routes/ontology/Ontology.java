package io.lumify.web.routes.ontology;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.ontology.Relationship;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Ontology extends BaseRequestHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public Ontology(
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        JSONObject resultJson = ontologyRepository.getJson();

        String eTag = generateETag(resultJson.toString().getBytes());
        if (testEtagHeaders(request, response, eTag)) {
            return;
        }

        addETagHeader(response, eTag);
        respondWithJson(response, resultJson);
    }
}
