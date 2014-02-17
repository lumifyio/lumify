package com.altamiracorp.lumify.web;

import com.altamiracorp.miniweb.App;
import com.altamiracorp.miniweb.Handler;
import com.google.inject.Injector;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;


public class WebApp extends App {
    private final Injector injector;

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
}
