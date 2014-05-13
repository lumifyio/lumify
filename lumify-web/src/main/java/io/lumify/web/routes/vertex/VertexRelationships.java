package io.lumify.web.routes.vertex;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexRelationships extends BaseRequestHandler {
    private final Graph graph;
    private final String artifactHasEntityIri;

    @Inject
    public VertexRelationships(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;

        this.artifactHasEntityIri = this.getConfiguration().get(Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY);
        if (this.artifactHasEntityIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        String graphVertexId = getAttributeString(request, "graphVertexId");
        long offset = getOptionalParameterLong(request, "offset", 0);
        long size = getOptionalParameterLong(request, "size", 25);

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response);
            return;
        }

        Iterable<Edge> edges = vertex.getEdges(Direction.BOTH, authorizations);

        JSONObject json = new JSONObject();
        JSONArray relationshipsJson = new JSONArray();
        long referencesAdded = 0, skipped = 0, totalReferences = 0;
        for (Edge edge : edges) {
            Vertex otherVertex = edge.getOtherVertex(vertex.getId(), authorizations);
            if (otherVertex == null) { // user doesn't have access to other side of edge
                continue;
            }

            if (edge.getLabel().equals(this.artifactHasEntityIri)) {
                totalReferences++;
                if (referencesAdded >= size) continue;
                if (skipped < offset) {
                    skipped++;
                    continue;
                }

                referencesAdded++;
            }

            JSONObject relationshipJson = new JSONObject();
            relationshipJson.put("relationship", JsonSerializer.toJson(edge, workspaceId));
            relationshipJson.put("vertex", JsonSerializer.toJson(otherVertex, workspaceId));
            relationshipsJson.put(relationshipJson);
        }
        json.put("totalReferences", totalReferences);
        json.put("relationships", relationshipsJson);

        respondWithJson(response, json);
    }
}
