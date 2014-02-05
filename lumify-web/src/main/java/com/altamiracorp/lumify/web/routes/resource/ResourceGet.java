package com.altamiracorp.lumify.web.routes.resource;

import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.GLYPH_ICON;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import java.io.ByteArrayOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

public class ResourceGet extends BaseRequestHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public ResourceGet(final OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String id = getAttributeString(request, "id");

        Concept concept = ontologyRepository.getConceptById(id);
        StreamingPropertyValue spv = GLYPH_ICON.getPropertyValue(concept.getVertex());

        if (spv == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
        IOUtils.copy(spv.getInputStream(), imgOut);

        byte[] rawImg = imgOut.toByteArray();

        // TODO change content type if we use this route for more than getting glyph icons
        response.setContentType("image/png");
        ServletOutputStream out = response.getOutputStream();
        out.write(rawImg);
        out.close();
    }
}
