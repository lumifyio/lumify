package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.Messaging;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.type.GeoPoint;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.GEO_LOCATION;

public class VertexSetProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexSetProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final UserRepository userRepository;

    @Inject
    public VertexSetProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final AuditRepository auditRepository,
            final VisibilityTranslator visibilityTranslator,
            final UserRepository userRepository) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String valueStr = getRequiredParameter(request, "value");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String justificationText = getOptionalParameter(request, "justificationText");
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");

        final JSONObject sourceJson;
        if (sourceInfo != null) {
            sourceJson = new JSONObject(sourceInfo);
        } else {
            sourceJson = new JSONObject();
        }

        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

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

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        ElementMutation<Vertex> graphVertexMutation = GraphUtil.setProperty(graphVertex, propertyName, value, visibilitySource, this.visibilityTranslator, justificationText, sourceJson);
        auditRepository.auditVertexElementMutation(graphVertexMutation, graphVertex, "", user, visibilityTranslator.toVisibility(visibilitySource));
        graphVertex = graphVertexMutation.save();
        graph.flush();

        Messaging.broadcastPropertyChange(graphVertexId, propertyName, value, toJson(graphVertex));

        JSONObject propertiesJson = GraphUtil.toJsonProperties(graphVertex.getProperties());
        JSONObject json = new JSONObject();
        json.put("properties", propertiesJson);

        if (toJson(graphVertex) != null) {
            json.put("vertex", toJson(graphVertex));
        }

        respondWithJson(response, json);
    }

    private JSONObject toJson(Vertex vertex) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("graphVertexId", vertex.getId());
            for (Property property : vertex.getProperties()) {
                if (GEO_LOCATION.getKey().equals(property.getName())) {
                    JSONObject geo = new JSONObject();
                    GeoPoint geoPoint = (GeoPoint) property.getValue();
                    geo.put("latitude", geoPoint.getLatitude());
                    geo.put("longitude", geoPoint.getLongitude());
                    obj.put(property.getName(), geo);
                } else {
                    obj.put(property.getName(), property.getValue()); // TODO handle mutivalued properties
                }
            }
            return obj;
        } catch (JSONException e) {
            new RuntimeException(e);
        }
        return null;
    }
}
