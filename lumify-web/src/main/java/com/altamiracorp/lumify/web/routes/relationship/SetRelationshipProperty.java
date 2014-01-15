package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Property;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.vertex.VertexProperties;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class SetRelationshipProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SetRelationshipProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;

    @Inject
    public SetRelationshipProperty(final OntologyRepository ontologyRepository, final Graph graph, final AuditRepository auditRepository) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String relationshipLabel = getRequiredParameter(request, "relationshipLabel");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String valueStr = getRequiredParameter(request, "value");
        final String sourceId = getRequiredParameter(request, "source");
        final String destId = getRequiredParameter(request, "dest");

        User user = getUser(request);
        Vertex sourceVertex = graph.getVertex(sourceId, user.getAuthorizations());
        Vertex destVertex = graph.getVertex(destId, user.getAuthorizations());

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
        for (Edge edge : sourceVertex.getEdges(destVertex, Direction.BOTH, relationshipLabel, user.getAuthorizations())) {
            Object oldValue = edge.getPropertyValue(propertyName, 0);
            edge.setProperty(propertyName, value, new Visibility(""));

            // TODO: replace "" when we implement commenting on ui
            auditRepository.auditRelationshipProperties(AuditAction.DELETE.toString(), sourceId, destId, propertyName, oldValue, edge, "", "", user);
        }

        // TODO get all properties from all edges?
        List<com.altamiracorp.securegraph.Property> properties = new ArrayList<com.altamiracorp.securegraph.Property>();
        Iterable<Edge> possibleEdges = sourceVertex.getEdges(destVertex, Direction.BOTH, relationshipLabel, user.getAuthorizations());
        for (Edge edge : possibleEdges) {
            for (com.altamiracorp.securegraph.Property p : edge.getProperties()) {
                properties.add(p);
            }
        }
        JSONObject resultsJson = VertexProperties.propertiesToJson(properties);

        respondWithJson(response, resultsJson);
    }
}
