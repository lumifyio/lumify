package io.lumify.web;

import com.altamiracorp.miniweb.App;
import com.altamiracorp.miniweb.Handler;
import com.google.inject.Injector;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.securegraph.util.CloseableUtils.closeQuietly;


public class WebApp extends App {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WebApp.class);
    private final Injector injector;
    private Map<String, String> javaScriptSources = new HashMap<String, String>();
    private Map<String, String> cssSources = new HashMap<String, String>();

    public WebApp(final ServletConfig servletConfig, final Injector injector) {
        super(servletConfig);

        this.injector = injector;
        Enumeration initParamNames = servletConfig.getInitParameterNames();
        while (initParamNames.hasMoreElements()) {
            String initParam = (String) initParamNames.nextElement();
            set(initParam, servletConfig.getInitParameter(initParam));
        }
    }

    @Override
    protected Handler[] instantiateHandlers(Class<? extends Handler>[] handlerClasses) throws Exception {
        Handler[] handlers = new Handler[handlerClasses.length];
        for (int i = 0; i < handlerClasses.length; i++) {
            handlers[i] = injector.getInstance(handlerClasses[i]);
        }
        return handlers;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getRequestURI().endsWith("ejs")) {
            response.setContentType("text/plain");
        }
        response.setCharacterEncoding("UTF-8");
        super.handle(request, response);
    }

    public void registerJavaScript(String scriptResourceName) {
        InputStream stream = WebApp.class.getResourceAsStream(scriptResourceName);
        if (stream == null) {
            throw new LumifyException("Could not find script resource: " + scriptResourceName);
        }
        try {
            LOGGER.info("registering JavaScript plugin file: %s", scriptResourceName);
            javaScriptSources.put(scriptResourceName, IOUtils.toString(stream, "UTF-8"));
        } catch (IOException e) {
            throw new LumifyException("Could not read script resource: " + scriptResourceName);
        } finally {
            closeQuietly(stream);
        }
    }

    public void registerCss(String cssResourceName) {
        InputStream stream = WebApp.class.getResourceAsStream(cssResourceName);
        if (stream == null) {
            throw new LumifyException("Could not find css resource: " + cssResourceName);
        }
        try {
            LOGGER.info("registering CSS plugin file: %s", cssResourceName);
            cssSources.put(cssResourceName, IOUtils.toString(stream, "UTF-8"));
        } catch (IOException e) {
            throw new LumifyException("Could not read css resource: " + cssResourceName);
        } finally {
            closeQuietly(stream);
        }
    }

    public Map<String, String> getJavaScriptSources() {
        return javaScriptSources;
    }

    public Map<String, String> getCssSources() {
        return cssSources;
    }
}
