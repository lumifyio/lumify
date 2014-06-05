package io.lumify.web.routes.plugins;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.WebApp;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class PluginsJavaScript extends BaseRequestHandler {
    @Inject
    public PluginsJavaScript(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        WebApp webApp = getWebApp(request);

        response.setHeader("Content-Type", "application/javascript");
        ServletOutputStream out = response.getOutputStream();
        for (Map.Entry<String, String> jsSource : webApp.getJavaScriptSources().entrySet()) {
            String start = "\n/* " + jsSource.getKey() + " */\n";
            out.write(start.getBytes());
            out.write(jsSource.getValue().getBytes());
        }
        out.close();
    }
}
