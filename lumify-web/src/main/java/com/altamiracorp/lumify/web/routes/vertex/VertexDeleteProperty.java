package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.PropertyJustificationMetadata;
import com.altamiracorp.lumify.core.model.PropertySourceMetadata;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VertexDeleteProperty extends BaseRequestHandler {
    private final Graph graph;
    private final AuditRepository auditRepository;

    @Inject
    public VertexDeleteProperty(
            final Graph graph,
            final AuditRepository auditRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String justificationText = getOptionalParameter(request, "justificationString");
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        Object oldValue = graphVertex.getProperty(propertyName);
        graphVertex.removeProperty(propertyName);
        graph.flush();

        Map<String, Object> metadata = new HashMap<String, Object>();
        if (justificationText != null) {
            metadata.put(PropertyJustificationMetadata.PROPERTY_JUSTIFICATION, new PropertyJustificationMetadata(justificationText));
        } else if (sourceInfo != null) {
            JSONObject sourceObject = new JSONObject(sourceInfo);
            int startOffset = sourceObject.getInt("startOffset");
            int endOffset = sourceObject.getInt("endOffset");
            String vertexId = sourceObject.getString("vertexId");
            String snippet = sourceObject.getString("snippet");
            PropertySourceMetadata sourceMetadata = new PropertySourceMetadata(startOffset, endOffset, vertexId, snippet);
            metadata.put(PropertySourceMetadata.PROPERTY_SOURCE_METADATA, sourceMetadata);
        }

        auditRepository.auditEntityProperty(AuditAction.DELETE, graphVertex, propertyName, oldValue, null, "", "", metadata, user, new LumifyVisibility().getVisibility());

        // TODO: broadcast property delete

        Iterable<com.altamiracorp.securegraph.Property> properties = graphVertex.getProperties();
        JSONObject propertiesJson = GraphUtil.toJsonProperties(properties);
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
