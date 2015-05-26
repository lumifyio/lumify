package io.lumify.vertexMenu;

import io.lumify.miniweb.Handler;
import io.lumify.miniweb.handlers.StaticResourceHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

public class VertexMenuWebAppPlugin implements WebAppPlugin {
    public static final String TERMS_OF_USE_PATH = "/terms";

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerJavaScript("/io/lumify/vertexMenu/vertex-menu-plugin.js");
    }
}
