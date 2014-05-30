package io.lumify.web.routes.config;

import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

        JSONObject results = new JSONObject();
        for (String key : getConfiguration().getKeys()) {
            if (key.startsWith(io.lumify.core.config.Configuration.WEB_PROPERTIES_PREFIX)) {
                results.put(key.replaceFirst(io.lumify.core.config.Configuration.WEB_PROPERTIES_PREFIX, ""), getConfiguration().get(key, ""));
            }
        }

        respondWithJson(response, results);
    }
}
