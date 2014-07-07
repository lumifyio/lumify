package io.lumify.web.routes.graph;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.ontology.PropertyType;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.query.GeoCompare;
import org.securegraph.type.GeoCircle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

public class GraphGeoLocationSearch extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public GraphGeoLocationSearch(
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

        JSONObject results = new JSONObject();
        JSONArray vertices = new JSONArray();

        for (OntologyProperty property : this.ontologyRepository.getProperties()) {
            if (property.getDataType() != PropertyType.GEO_LOCATION) {
                continue;
            }

            Iterator<Vertex> vertexIterator = graph.query(authorizations).
                    has(property.getTitle(), GeoCompare.WITHIN, new GeoCircle(latitude, longitude, radius)).
                    vertices().
                    iterator();
            while (vertexIterator.hasNext()) {
                vertices.put(JsonSerializer.toJson(vertexIterator.next(), workspaceId));
            }
        }

        results.put("vertices", vertices);

        respondWithJson(response, results);
    }
}
