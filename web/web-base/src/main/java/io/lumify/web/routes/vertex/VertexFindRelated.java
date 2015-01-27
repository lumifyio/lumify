package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiVertexFindRelatedResponse;
import org.securegraph.Authorizations;
import org.securegraph.Direction;
import org.securegraph.Graph;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VertexFindRelated extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexFindRelated(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
    }


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] graphVertexIds = getRequiredParameterArray(request, "graphVertexIds[]");
        String limitParentConceptId = getOptionalParameter(request, "limitParentConceptId");
        String limitEdgeLabel = getOptionalParameter(request, "limitEdgeLabel");
        long maxVerticesToReturn = getOptionalParameterLong(request, "maxVerticesToReturn", 250);

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Set<String> limitConceptIds = new HashSet<>();

        if (limitParentConceptId != null) {
            List<Concept> limitConcepts = ontologyRepository.getConceptAndChildrenByIRI(limitParentConceptId);
            if (limitConcepts == null) {
                throw new RuntimeException("Bad 'limitParentConceptId', no concept found for id: " + limitParentConceptId);
            }
            for (Concept con : limitConcepts) {
                limitConceptIds.add(con.getIRI());
            }
        }

        Set<String> visitedIds = new HashSet<>();

        ClientApiVertexFindRelatedResponse result = new ClientApiVertexFindRelatedResponse();
        long count = visitedIds.size();
        for (String id : graphVertexIds) {
            Iterable<Vertex> vertices = graph.getVertex(id, authorizations).getVertices(Direction.BOTH, limitEdgeLabel, authorizations);
            for (Vertex vertex : vertices) {
                if (!visitedIds.add(vertex.getId())) continue;
                if (limitConceptIds.size() == 0 || !isLimited(vertex, limitConceptIds)) {
                    if (count < maxVerticesToReturn) {
                        result.getVertices().add(ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations));
                    }
                    count++;
                }
            }
        }

        result.setCount(count);

        respondWithClientApiObject(response, result);
    }

    private boolean isLimited(Vertex vertex, Set<String> limitConceptIds) {
        String conceptId = LumifyProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        return !limitConceptIds.contains(conceptId);
    }
}

