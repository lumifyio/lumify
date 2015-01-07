package io.lumify.web.routes.config;

import com.google.inject.Inject;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ResourceBundle;

public class Configuration extends BaseRequestHandler {

    @Inject
    public Configuration(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final io.lumify.core.config.Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        ResourceBundle resourceBundle = getBundle(request);
        JSONObject configuration = getConfiguration().toJSON(resourceBundle);

        respondWithJson(response, configuration);
    }
}
