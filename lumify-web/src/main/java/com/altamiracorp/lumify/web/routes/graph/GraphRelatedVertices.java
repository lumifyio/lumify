package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Direction;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;

public class GraphRelatedVertices extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public GraphRelatedVertices(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = getAttributeString(request, "graphVertexId");
        String limitParentConceptId = getOptionalParameter(request, "limitParentConceptId");
        long maxVerticesToReturn = getOptionalParameterLong(request, "maxVerticesToReturn", 250);

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getWorkspaceId(request);

        Set<String> limitConceptIds = new HashSet<String>();

        if (limitParentConceptId != null) {
            List<Concept> limitConcepts = ontologyRepository.getConceptByIdAndChildren(limitParentConceptId);
            if (limitConcepts == null) {
                throw new RuntimeException("Bad 'limitParentConceptId', no concept found for id: " + limitParentConceptId);
            }
            for (Concept con : limitConcepts) {
                limitConceptIds.add(con.getId());
            }
        }

        Iterable<Vertex> vertices = graph.getVertex(graphVertexId, authorizations)
                .getVertices(Direction.BOTH, authorizations);

        JSONObject json = new JSONObject();
        JSONArray verticesJson = new JSONArray();
        long count = 0;
        for (Vertex vertex : vertices) {
            if (limitConceptIds.size() == 0 || !isLimited(limitConceptIds, vertex)) {
                if (count < maxVerticesToReturn) {
                    verticesJson.put(GraphUtil.toJson(vertex, workspaceId));
                }
                count++;
            }
        }
        json.put("count", count);
        json.put("vertices", verticesJson);

        respondWithJson(response, json);

        chain.next(request, response);
    }

    private boolean isLimited(Set<String> limitConceptIds, Vertex vertex) {
        String conceptId = CONCEPT_TYPE.getPropertyValue(vertex);
        return !limitConceptIds.contains(conceptId);
    }
}

