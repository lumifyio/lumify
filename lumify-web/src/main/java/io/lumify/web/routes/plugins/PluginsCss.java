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

public class PluginsCss extends BaseRequestHandler {
    @Inject
    public PluginsCss(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        WebApp webApp = getWebApp(request);

        response.setHeader("Content-Type", "text/css");
        ServletOutputStream out = response.getOutputStream();
        for (String cssSource : webApp.getCssSources()) {
            out.write(cssSource.getBytes());
            out.write("\n\n/******************************************************************************************************/\n\n".getBytes());
        }
        out.close();
    }
}
