package io.lumify.web.routes;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ServletContextTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.MinimalRequestHandler;
import io.lumify.web.WebApp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Index extends MinimalRequestHandler {
    private static final String PLUGIN_JS_RESOURCES_PARAM = "pluginJsResources";
    private static final String PLUGIN_CSS_RESOURCES_PARAM = "pluginCssResources";
    private static final Map<String, String> MESSAGE_BUNDLE_PARAMS = ImmutableMap.of(
            "title",       "lumify.title",
            "description", "lumify.description"
    );

    private String indexHtml;

    @Inject
    protected Index(Configuration configuration) {
        super(configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        response.setContentType("text/html");
        response.getWriter().write(getIndexHtml(request));
    }

    private String getIndexHtml(HttpServletRequest request) throws IOException {
        if (indexHtml == null) {
            WebApp app = getWebApp(request);
            Map<String, Object> context = new HashMap<String, Object>();
            context.put(PLUGIN_JS_RESOURCES_PARAM, app.getPluginsJsResources());
            context.put(PLUGIN_CSS_RESOURCES_PARAM, app.getPluginsCssResources());
            for (Map.Entry<String, String> param : MESSAGE_BUNDLE_PARAMS.entrySet()) {
                context.put(param.getKey(), getString(request, param.getValue()));
            }
            TemplateLoader templateLoader = new ServletContextTemplateLoader(request.getServletContext(), "/", ".hbs");
            Handlebars handlebars = new Handlebars(templateLoader);
            Template template = handlebars.compile("index");
            indexHtml = template.apply(context);
        }
        return indexHtml;
    }
}
