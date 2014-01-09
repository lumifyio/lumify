package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Property;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.vertex.VertexProperties;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import com.tinkerpop.blueprints.Edge;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class DeleteRelationshipProperty extends BaseRequestHandler {
    private final GraphRepository graphRepository;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;

    @Inject
    public DeleteRelationshipProperty(final OntologyRepository ontologyRepo, final GraphRepository graphRepo, final AuditRepository auditRepo) {
        ontologyRepository = ontologyRepo;
        graphRepository = graphRepo;
        auditRepository = auditRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String relationshipLabel = getRequiredParameter(request, "relationshipLabel");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String sourceId = getRequiredParameter(request, "source");
        final String destId = getRequiredParameter(request, "dest");

        User user = getUser(request);

        Property property = ontologyRepository.getProperty(propertyName, user);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        Edge edge = graphRepository.findEdge(sourceId, destId, relationshipLabel, user);
        Object oldValue = edge.getProperty(propertyName);
        edge.removeProperty(propertyName);
        graphRepository.commit();

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationshipProperties(AuditAction.DELETE.toString(), sourceId, destId, property.getDisplayName(), oldValue, edge, "", "", user);

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
