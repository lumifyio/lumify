package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Property;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class VertexDeleteProperty extends BaseRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(VertexDeleteProperty.class);

    private final GraphRepository graphRepository;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;

    @Inject
    public VertexDeleteProperty(final OntologyRepository ontologyRepo, final GraphRepository graphRepo, final AuditRepository auditRepo) {
        ontologyRepository = ontologyRepo;
        graphRepository = graphRepo;
        auditRepository = auditRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");

        User user = getUser(request);
        Property property = ontologyRepository.getProperty(propertyName, user);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        GraphVertex graphVertex = graphRepository.findVertex(graphVertexId, user);
        graphVertex.removeProperty(propertyName);

        graphRepository.save(graphVertex, user);

        // TODO: add auditing
        // TODO: broadcast property delete

        Map<String, String> properties = graphRepository.getVertexProperties(graphVertexId, user);
        JSONObject propertiesJson = VertexProperties.propertiesToJson(properties);
        JSONObject json = new JSONObject();
        json.put("properties", propertiesJson);
        json.put("deletedProperty", propertyName);

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
