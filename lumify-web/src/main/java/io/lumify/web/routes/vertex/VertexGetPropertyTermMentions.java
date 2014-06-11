package io.lumify.web.routes.vertex;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.termMention.TermMentionModel;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Property;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexGetPropertyTermMentions extends BaseRequestHandler {
    private final Graph graph;
    private final UserRepository userRepository;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexGetPropertyTermMentions(
            Graph graph,
            UserRepository userRepository,
            Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            TermMentionRepository termMentionRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.userRepository = userRepository;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = getRequiredParameter(request, "graphVertexId");
        String propertyName = getRequiredParameter(request, "propertyName");
        String propertyKey = getRequiredParameter(request, "propertyKey");

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

        Iterable<TermMentionModel> termMentions = termMentionRepository.findByGraphVertexIdAndPropertyKey(vertex.getId().toString(), propertyKey, modelUserContext);
        JSONObject json = new JSONObject();
        JSONArray termMentionsJson = termMentionsToJson(termMentions);
        json.put("termMentions", termMentionsJson);
        respondWithJson(response, json);
    }

    private ModelUserContext getModelUserContext(HttpServletRequest request, Authorizations authorizations) {
        String workspaceId = getWorkspaceIdOrDefault(request);
        ModelUserContext modelUserContext;
        if (workspaceId == null) {
            modelUserContext = userRepository.getModelUserContext(authorizations);
        } else {
            modelUserContext = userRepository.getModelUserContext(authorizations, workspaceId);
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
