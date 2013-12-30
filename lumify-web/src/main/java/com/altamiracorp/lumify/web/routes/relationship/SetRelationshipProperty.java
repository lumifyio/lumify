package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Property;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.vertex.VertexProperties;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class SetRelationshipProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SetRelationshipProperty.class);

    private final GraphRepository graphRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public SetRelationshipProperty(final OntologyRepository ontologyRepo, final GraphRepository graphRepo) {
        ontologyRepository = ontologyRepo;
        graphRepository = graphRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String relationshipLabel = getRequiredParameter(request, "relationshipLabel");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String valueStr = getRequiredParameter(request, "value");
        final String sourceId = getRequiredParameter(request, "source");
        final String destId = getRequiredParameter(request, "dest");

        User user = getUser(request);

        Property property = ontologyRepository.getProperty(propertyName, user);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        Object value;
        try {
            value = property.convertString(valueStr);
        } catch (Exception ex) {
            LOGGER.warn(String.format("Validation error propertyName: %s, valueStr: %s", propertyName, valueStr), ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }

        graphRepository.setPropertyEdge(sourceId, destId, relationshipLabel, propertyName, value, user);

        Map<String, String> properties = graphRepository.getEdgeProperties(sourceId, destId, relationshipLabel, user);
        for (Map.Entry<String, String> p : properties.entrySet()) {
            String displayName = ontologyRepository.getDisplayNameForLabel(p.getValue(), user);
            if (displayName != null) {
                p.setValue(displayName);
            }
        }
        JSONObject resultsJson = VertexProperties.propertiesToJson(properties);

        respondWithJson(response, resultsJson);
    }
}
