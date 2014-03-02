package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

public class VertexProperties extends BaseRequestHandler {
    private final Graph graph;
    private final UserRepository userRepository;
    private final DetectedObjectRepository detectedObjectRepository;

    @Inject
    public VertexProperties(final Graph graph, final UserRepository userRepository, final DetectedObjectRepository detectedObjectRepository) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.detectedObjectRepository = detectedObjectRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

        Iterable<Property> properties = graph.getVertex(graphVertexId, authorizations).getProperties();
        JSONObject propertiesJson = GraphUtil.toJsonProperties(properties);

        JSONObject json = new JSONObject();
        json.put("id", graphVertexId);
        json.put("properties", propertiesJson);

        Iterator<DetectedObjectModel> detectedObjectModels = detectedObjectRepository.findByGraphVertexId(graphVertexId, user).iterator();
        JSONArray detectedObjects = new JSONArray();
        while (detectedObjectModels.hasNext()) {
            DetectedObjectModel detectedObjectModel = detectedObjectModels.next();
            JSONObject detectedObjectModelJson = detectedObjectModel.toJson();
            if (detectedObjectModel.getMetadata().getResolvedId() != null) {
                detectedObjectModelJson.put("entityVertex", GraphUtil.toJson(graph.getVertex(detectedObjectModel.getMetadata().getResolvedId(), authorizations)));
            }
            detectedObjects.put(detectedObjectModelJson);
        }
        json.put("detectedObjects", detectedObjects);

        respondWithJson(response, json);
    }
}
