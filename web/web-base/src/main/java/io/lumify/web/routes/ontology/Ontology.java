package io.lumify.web.routes.ontology;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiOntology;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;

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
        ClientApiOntology result = ontologyRepository.getClientApiObject();

        String json = ObjectMapperFactory.getInstance().writeValueAsString(result);
        String eTag = generateETag(json.getBytes());
        if (testEtagHeaders(request, response, eTag)) {
            return;
        }

        addETagHeader(response, eTag);
        respondWithClientApiObject(response, result);
    }
}
