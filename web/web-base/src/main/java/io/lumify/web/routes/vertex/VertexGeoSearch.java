package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiVertexSearchResponse;
import io.lumify.web.clientapi.model.PropertyType;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.query.GeoCompare;
import org.securegraph.type.GeoCircle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexGeoSearch extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexGeoSearch(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final OntologyRepository ontologyRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final double latitude = getRequiredParameterAsDouble(request, "lat");
        final double longitude = getRequiredParameterAsDouble(request, "lon");
        final double radius = getRequiredParameterAsDouble(request, "radius");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        ClientApiVertexSearchResponse results = new ClientApiVertexSearchResponse();

        for (OntologyProperty property : this.ontologyRepository.getProperties()) {
            if (property.getDataType() != PropertyType.GEO_LOCATION) {
                continue;
            }

            Iterable<Vertex> vertices = graph.query(authorizations).
                    has(property.getTitle(), GeoCompare.WITHIN, new GeoCircle(latitude, longitude, radius)).
                    vertices();
            for (Vertex vertex : vertices) {
                results.getVertices().add(ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations));
            }
        }

        respondWithClientApiObject(response, results);
    }
}
