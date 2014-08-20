package io.lumify.web.routes.resource;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.BaseRequestHandler;
import io.lumify.miniweb.HandlerChain;
import com.google.inject.Inject;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceGet extends BaseRequestHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public ResourceGet(
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String id = getAttributeString(request, "id");

        Concept concept = ontologyRepository.getConceptByIRI(id);
        byte[] rawImg = concept.getGlyphIcon();

        if (rawImg == null || rawImg.length <= 0) {
            respondWithNotFound(response);
            return;
        }

        // TODO change content type if we use this route for more than getting glyph icons
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "max-age=" + (5 * 60));
        ServletOutputStream out = response.getOutputStream();
        out.write(rawImg);
        out.close();
    }
}
