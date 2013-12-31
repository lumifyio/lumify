package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Property;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.Messaging;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public class VertexSetProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexSetProperty.class);

    private final GraphRepository graphRepository;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;

    @Inject
    public VertexSetProperty(final OntologyRepository ontologyRepo, final GraphRepository graphRepo, final AuditRepository auditRepo) {
        ontologyRepository = ontologyRepo;
        graphRepository = graphRepo;
        auditRepository = auditRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String valueStr = getRequiredParameter(request, "value");

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

        GraphVertex graphVertex = graphRepository.findVertex(graphVertexId, user);

        List<String> modifiedProperties = Lists.newArrayList(propertyName);
        graphVertex.setProperty(propertyName, value);

        if (propertyName.equals(PropertyName.GEO_LOCATION.toString())) {
            graphVertex.setProperty(PropertyName.GEO_LOCATION_DESCRIPTION, "");
            modifiedProperties.add(PropertyName.GEO_LOCATION_DESCRIPTION.toString());
        } else if (propertyName.equals(PropertyName.SOURCE.toString())) {
            graphVertex.setProperty(PropertyName.SOURCE, value);
            modifiedProperties.add(PropertyName.SOURCE.toString());
        }
        graphRepository.save(graphVertex, user);

        for (String modifiedProperty : modifiedProperties) {
            // TODO: replace second "" when we implement commenting on ui
            auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), graphVertex, modifiedProperty, "", "", user);
        }

        Messaging.broadcastPropertyChange(graphVertexId, propertyName, value, toJson(graphVertex));

        Map<String, String> properties = graphRepository.getVertexProperties(graphVertexId, user);
        JSONObject propertiesJson = VertexProperties.propertiesToJson(properties);
        JSONObject json = new JSONObject();
        json.put("properties", propertiesJson);

        if (toJson(graphVertex) != null) {
            json.put("vertex", toJson(graphVertex));
        }

        respondWithJson(response, json);
    }

    private JSONObject toJson(GraphVertex vertex) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("graphVertexId", vertex.getId());
            for (String propertyKey : vertex.getPropertyKeys()) {
                obj.put(propertyKey, vertex.getProperty(propertyKey));
            }
            return obj;
        } catch (JSONException e) {
            new RuntimeException(e);
        }
        return null;
    }
}
