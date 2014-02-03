package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.ElementMutation;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SetRelationshipProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SetRelationshipProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;
    private VisibilityTranslator visibilityTranslator;

    @Inject
    public SetRelationshipProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final AuditRepository auditRepository,
            final VisibilityTranslator visibilityTranslator) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String valueStr = getRequiredParameter(request, "value");
        final String sourceId = getRequiredParameter(request, "source");
        final String destId = getRequiredParameter(request, "dest");
        final String edgeId = getRequiredParameter(request, "edgeId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");

        User user = getUser(request);

        OntologyProperty property = ontologyRepository.getProperty(propertyName);
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
        Edge edge = graph.getEdge(edgeId, user.getAuthorizations());
        Object oldValue = edge.getPropertyValue(propertyName, 0);
        ElementMutation<Edge> graphEdgeMutation = GraphUtil.setProperty(edge, propertyName, value, visibilitySource, this.visibilityTranslator);
        graphEdgeMutation.save();

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationshipProperties(AuditAction.DELETE.toString(), sourceId, destId, propertyName, oldValue, edge, "", "", user);

        JSONObject resultsJson = GraphUtil.toJson(edge.getProperties());
        respondWithJson(response, resultsJson);
    }
}
