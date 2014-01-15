package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

public class VertexDeleteProperty extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;

    @Inject
    public VertexDeleteProperty(final OntologyRepository ontologyRepo, final Graph graphRepo, final AuditRepository auditRepo) {
        ontologyRepository = ontologyRepo;
        graph = graphRepo;
        auditRepository = auditRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");

        User user = getUser(request);
        OntologyProperty property = ontologyRepository.getProperty(propertyName, user);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        Vertex graphVertex = graph.getVertex(graphVertexId, user.getAuthorizations());
        graphVertex.removeProperty(property.getId().toString(), propertyName);

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditEntityProperties(AuditAction.DELETE.toString(), graphVertex, propertyName, "", "", user);

        // TODO: broadcast property delete

        Iterable<com.altamiracorp.securegraph.Property> properties = graphVertex.getProperties();
        JSONObject propertiesJson = VertexProperties.propertiesToJson(properties);
        JSONObject json = new JSONObject();
        json.put("properties", propertiesJson);
        json.put("deletedProperty", propertyName);

        if (toJson(graphVertex) != null) {
            json.put("vertex", toJson(graphVertex));
        }

        respondWithJson(response, json);
    }

    private JSONObject toJson(Vertex vertex) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("graphVertexId", vertex.getId());
            Iterator<com.altamiracorp.securegraph.Property> propertyIterator = vertex.getProperties().iterator();
            while (propertyIterator.hasNext()) {
                com.altamiracorp.securegraph.Property property = propertyIterator.next();
                obj.put(property.getName(), property.getValue());
            }
            return obj;
        } catch (JSONException e) {
            new RuntimeException(e);
        }
        return null;
    }
}
