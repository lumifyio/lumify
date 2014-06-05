package io.lumify.web;

import com.altamiracorp.miniweb.App;
import com.altamiracorp.miniweb.Handler;
import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;
import org.securegraph.SecureGraphException;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


public class WebApp extends App {
    private final Injector injector;
    private List<String> javaScriptSources = new ArrayList<String>();
    private List<String> cssSources = new ArrayList<String>();

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
            throw new SecureGraphException("Could not find script resource: " + scriptResourceName);
        }
        try {
            javaScriptSources.add(IOUtils.toString(stream));
        } catch (IOException e) {
            throw new SecureGraphException("Could not read script resource: " + scriptResourceName);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public void registerCss(String cssResourceName) {
        InputStream stream = WebApp.class.getResourceAsStream(cssResourceName);
        if (stream == null) {
            throw new SecureGraphException("Could not find css resource: " + cssResourceName);
        }
        try {
            cssSources.add(IOUtils.toString(stream));
        } catch (IOException e) {
            throw new SecureGraphException("Could not read css resource: " + cssResourceName);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public Iterable<String> getJavaScriptSources() {
        return javaScriptSources;
    }

    public Iterable<String> getCssSources() {
        return cssSources;
    }
}
