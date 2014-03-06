package com.altamiracorp.lumify.web.routes.config;

import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.Responder;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;

public class Plugin extends BaseRequestHandler {
    private static final String WEB_PLUGINS_PREFIX = "web.plugins.";
    private static final String DEFAULT_PLUGINS_DIR = "/jsc/configuration/plugins";

    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Plugin.class);

    private final com.altamiracorp.lumify.core.config.Configuration configuration;

    @Inject
    public Plugin(final com.altamiracorp.lumify.core.config.Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String pluginName = getAttributeString(request, "pluginName");
        final String configurationKey = WEB_PLUGINS_PREFIX + pluginName;
        String pluginPath = configuration.get(configurationKey, null);

        // Default behavior if not customized
        if (pluginPath == null) {
            pluginPath = request.getServletContext().getResource(DEFAULT_PLUGINS_DIR + "/" + pluginName).getPath();
        }

        String uri = request.getRequestURI();
        String searchString = "/" + pluginName + "/";
        String pluginResourcePath = uri.substring(uri.indexOf(searchString) + searchString.length());

        if (pluginResourcePath.endsWith(".js")) {
            response.setContentType("application/x-javascript");
        } else if (pluginResourcePath.endsWith(".ejs")) {
            response.setContentType("text/plain");
        } else if (pluginResourcePath.endsWith(".css")) {
            response.setContentType("text/css");
        } else if (pluginResourcePath.endsWith(".html")) {
            response.setContentType("text/html");
        } else {
            LOGGER.error("Only js,ejs,css,html files served from plugin");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String filePath = FilenameUtils.concat(pluginPath, pluginResourcePath);
        File file = new File(filePath);

        if (file.exists()) {
            response.setCharacterEncoding("UTF-8");
            FileUtils.copyFile(file, response.getOutputStream());
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
