package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class DeleteRelationshipProperty extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    @Inject
    public DeleteRelationshipProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final AuditRepository auditRepository,
            final UserRepository userRepository) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        Visibility visibility = new Visibility("");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String sourceId = getRequiredParameter(request, "source");
        final String destId = getRequiredParameter(request, "dest");
        final String edgeId = getRequiredParameter(request, "edgeId");

        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

        OntologyProperty property = ontologyRepository.getProperty(propertyName);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        // TODO remove all properties from all edges? I don't think so
        Edge edge = graph.getEdge(edgeId, authorizations);
        Object oldValue = edge.getPropertyValue(propertyName, 0);
        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationshipProperty(AuditAction.DELETE, sourceId, destId, property.getDisplayName(), oldValue, edge, "", "", user, visibility);
        edge.removeProperty(propertyName);
        graph.flush();

        List<Property> properties = new ArrayList<Property>();
        for (Property p : edge.getProperties()) {
            properties.add(p);
        }
        JSONObject resultsJson = GraphUtil.toJsonProperties(properties);

        respondWithJson(response, resultsJson);
    }
}
