package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexGetPropertyTermMentions extends BaseRequestHandler {
    private final Graph graph;
    private final UserProvider userProvider;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexGetPropertyTermMentions(
            Graph graph,
            UserRepository userRepository,
            Configuration configuration,
            UserProvider userProvider,
            TermMentionRepository termMentionRepository) {
        super(userRepository, configuration);
        this.graph = graph;
        this.userProvider = userProvider;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));
        String propertyName = UrlUtils.urlDecode(getAttributeString(request, "propertyName"));
        String propertyKey = UrlUtils.urlDecode(getAttributeString(request, "propertyKey"));

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        ModelUserContext modelUserContext = getModelUserContext(request, authorizations);

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response, String.format("vertex %s not found", graphVertexId));
            return;
        }

        Property property = vertex.getProperty(propertyKey, propertyName);
        if (property == null) {
            respondWithNotFound(response, String.format("property %s:%s not found on vertex %s", propertyKey, propertyName, vertex.getId()));
            return;
        }

        Iterable<TermMentionModel> termMentions = termMentionRepository.findByGraphVertexId(vertex.getId().toString(), modelUserContext);
        JSONObject json = new JSONObject();
        JSONArray termMentionsJson = termMentionsToJson(termMentions);
        json.put("termMentions", termMentionsJson);
        respondWithJson(response, json);
    }

    private ModelUserContext getModelUserContext(HttpServletRequest request, Authorizations authorizations) {
        String workspaceId = getWorkspaceIdOrDefault(request);
        ModelUserContext modelUserContext;
        if (workspaceId == null) {
            modelUserContext = this.userProvider.getModelUserContext(authorizations);
        } else {
            modelUserContext = this.userProvider.getModelUserContext(authorizations, workspaceId);
        }
        return modelUserContext;
    }

    private JSONArray termMentionsToJson(Iterable<TermMentionModel> termMentions) {
        JSONArray termMentionsJson = new JSONArray();
        for (TermMentionModel termMention : termMentions) {
            termMentionsJson.put(termMention.toJson());
        }
        return termMentionsJson;
    }
}
