package io.lumify.web.routes;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ServletContextTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.lumify.miniweb.Handler;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.WebApp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Index implements Handler {
    public static final String PLUGIN_JS_RESOURCES_PARAM = "pluginJsResources";
    public static final String PLUGIN_CSS_RESOURCES_PARAM = "pluginCssResources";
    private String indexHtml;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        response.setContentType("text/html");
        response.getWriter().write(getIndexHtml(request));
    }

    private String getIndexHtml(HttpServletRequest request) throws IOException {
        if (indexHtml == null) {
            WebApp app = (WebApp) WebApp.getApp(request);
            Map<String, List<String>> context = new HashMap<String, List<String>>();
            context.put(PLUGIN_JS_RESOURCES_PARAM, app.getPluginsJsResources());
            context.put(PLUGIN_CSS_RESOURCES_PARAM, app.getPluginsCssResources());
            TemplateLoader templateLoader = new ServletContextTemplateLoader(request.getServletContext(), "/", ".hbs");
            Handlebars handlebars = new Handlebars(templateLoader);
            Template template = handlebars.compile("index");
            indexHtml = template.apply(context);
        }
        return indexHtml;
    }
}
