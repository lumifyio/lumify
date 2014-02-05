package com.altamiracorp.lumify.web.routes.vertex;

import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.*;

import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.Messaging;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.ElementMutation;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.type.GeoPoint;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class VertexSetProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexSetProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;

    @Inject
    public VertexSetProperty(final OntologyRepository ontologyRepo, final Graph graph, final AuditRepository auditRepo) {
        this.ontologyRepository = ontologyRepo;
        this.graph = graph;
        this.auditRepository = auditRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String valueStr = getRequiredParameter(request, "value");

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
        ElementMutation<Vertex> graphVertexMutation = graphVertex.prepareMutation();

        Visibility visibility = new Visibility(""); // TODO set visibility
        graphVertexMutation.setProperty(propertyName, value, visibility); // TODO should we wrap with Text

        if (GEO_LOCATION.getKey().equals(propertyName)) {
            String[] latlong = valueStr.substring(valueStr.indexOf('(') + 1, valueStr.indexOf(')')).split(",");
            GeoPoint geoPoint = new GeoPoint(Double.parseDouble(latlong[0]), Double.parseDouble(latlong[1]));
            GEO_LOCATION.setProperty(graphVertexMutation, geoPoint, visibility);
            GEO_LOCATION_DESCRIPTION.setProperty(graphVertexMutation, "", visibility);
        } else if (SOURCE.getKey().equals(propertyName)) {
            SOURCE.setProperty(graphVertexMutation, value.toString(), visibility);
        }

        auditRepository.auditVertexElementMutation(graphVertexMutation, graphVertex, "", user);
        graphVertex = graphVertexMutation.save();
        graph.flush();

        Messaging.broadcastPropertyChange(graphVertexId, propertyName, value, toJson(graphVertex));

        JSONObject propertiesJson = VertexProperties.propertiesToJson(graphVertex.getProperties());
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
