package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Property;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.vertex.VertexProperties;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import com.tinkerpop.blueprints.Edge;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class SetRelationshipProperty extends BaseRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetRelationshipProperty.class);

    private final GraphRepository graphRepository;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;

    @Inject
    public SetRelationshipProperty(final OntologyRepository ontologyRepo, final GraphRepository graphRepo, final AuditRepository auditRepo) {
        ontologyRepository = ontologyRepo;
        graphRepository = graphRepo;
        auditRepository = auditRepo;
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
            LOGGER.warn("Validation error propertyName: " + propertyName + ", valueStr: " + valueStr, ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }
        Edge edge = graphRepository.findEdge(sourceId, destId, relationshipLabel, user);
        Object oldValue = edge.getProperty(propertyName);

        graphRepository.setPropertyEdge(sourceId, destId, relationshipLabel, propertyName, value, user);

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationshipProperties(AuditAction.DELETE.toString(), sourceId, destId, propertyName, oldValue, edge, "", "", user);

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
