package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.Messaging;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.type.GeoPoint;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class VertexSetProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexSetProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;
    private VisibilityTranslator visibilityTranslator;

    @Inject
    public VertexSetProperty(
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
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String valueStr = getRequiredParameter(request, "value");
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

        Vertex graphVertex = graph.getVertex(graphVertexId, user.getAuthorizations());
        Property oldProperty = graphVertex.getProperty(propertyName);
        Map<String, Object> propertyMetadata;
        if (oldProperty != null) {
            propertyMetadata = oldProperty.getMetadata();
        } else {
            propertyMetadata = new HashMap<String, Object>();
        }
        ElementMutation<Vertex> graphVertexMutation = graphVertex.prepareMutation();

        Visibility visibility = visibilityTranslator.toVisibility(visibilitySource);
        propertyMetadata.put(PropertyName.VISIBILITY_SOURCE.toString(), visibilitySource);

        if (propertyName.equals(PropertyName.GEO_LOCATION.toString())) {
            String[] latlong = valueStr.substring(valueStr.indexOf('(') + 1, valueStr.indexOf(')')).split(",");
            GeoPoint geoPoint = new GeoPoint(Double.parseDouble(latlong[0]), Double.parseDouble(latlong[1]));
            graphVertexMutation.setProperty(PropertyName.GEO_LOCATION.toString(), geoPoint, propertyMetadata, visibility);
            graphVertexMutation.setProperty(PropertyName.GEO_LOCATION_DESCRIPTION.toString(), "", propertyMetadata, visibility);
        } else {
            graphVertexMutation.setProperty(propertyName, value, propertyMetadata, visibility);
        }

        auditRepository.auditVertexElementMutation(graphVertexMutation, graphVertex, "", user);
        graphVertex = graphVertexMutation.save();
        graph.flush();

        Messaging.broadcastPropertyChange(graphVertexId, propertyName, value, toJson(graphVertex));

        JSONObject propertiesJson = GraphUtil.toJson(graphVertex.getProperties());
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
                if (property.getName().equals(PropertyName.GEO_LOCATION.toString())) {
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
