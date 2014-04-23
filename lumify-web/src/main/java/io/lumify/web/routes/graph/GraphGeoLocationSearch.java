package io.lumify.web.routes.graph;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.query.GeoCompare;
import com.altamiracorp.securegraph.type.GeoCircle;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

import static io.lumify.core.model.properties.EntityLumifyProperties.GEO_LOCATION;

public class GraphGeoLocationSearch extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public GraphGeoLocationSearch(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final double latitude = getRequiredParameterAsDouble(request, "lat");
        final double longitude = getRequiredParameterAsDouble(request, "lon");
        final double radius = getRequiredParameterAsDouble(request, "radius");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Iterator<Vertex> vertexIterator = graph.query(authorizations).
                has(GEO_LOCATION.getKey(), GeoCompare.WITHIN, new GeoCircle(latitude, longitude, radius)).
                vertices().
                iterator();

        JSONObject results = new JSONObject();
        JSONArray vertices = new JSONArray();
        while (vertexIterator.hasNext()) {
            vertices.put(JsonSerializer.toJson(vertexIterator.next(), workspaceId));
        }

        results.put("vertices", vertices);

        respondWithJson(response, results);
    }
}
