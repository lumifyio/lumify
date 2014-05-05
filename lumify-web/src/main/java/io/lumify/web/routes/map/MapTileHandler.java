package io.lumify.web.routes.map;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class MapTileHandler extends BaseRequestHandler {
    @Inject
    public MapTileHandler(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration config) {
        super(userRepository, workspaceRepository, config);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String hostName = getConfiguration().get(Configuration.MAP_TILE_SERVER_HOST);
        int port = getConfiguration().getInt(Configuration.MAP_TILE_SERVER_PORT);

        final String x = getAttributeString(request, "x");
        final String y = getAttributeString(request, "y");
        final String z = getAttributeString(request, "z");

        String path = "/" + z + "/" + x + "/" + y + ".png";
        URL url = new URL("http", hostName, port, path);

        InputStream in = url.openStream();
        OutputStream out = response.getOutputStream();
        response.setContentType("image/png");
        IOUtils.copy(in, out);
        out.close();
    }
}
