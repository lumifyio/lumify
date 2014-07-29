package io.lumify.web.routes.config;

import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.web.WebApp;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
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
        JSONObject properties = new JSONObject();
        for (String key : getConfiguration().getKeys()) {
            if (key.startsWith(io.lumify.core.config.Configuration.WEB_PROPERTIES_PREFIX)) {
                properties.put(key.replaceFirst(io.lumify.core.config.Configuration.WEB_PROPERTIES_PREFIX, ""), getConfiguration().get(key, ""));
            } else if (key.startsWith(io.lumify.core.config.Configuration.ONTOLOGY_IRI_PREFIX)) {
                properties.put(key, getConfiguration().get(key, ""));
            }
        }

        JSONObject messages = new JSONObject();
        ResourceBundle resourceBundle = getBundle(request);
        for (String key : resourceBundle.keySet()) {
            messages.put(key, resourceBundle.getString(key));
        }

        JSONObject configuration = new JSONObject();
        configuration.put("properties", properties);
        configuration.put("messages", messages);

        respondWithJson(response, configuration);
    }
}
