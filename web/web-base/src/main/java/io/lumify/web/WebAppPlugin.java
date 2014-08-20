package io.lumify.web;

import io.lumify.miniweb.Handler;

import javax.servlet.ServletContext;

public interface WebAppPlugin {
    void init(WebApp app, ServletContext servletContext, Handler authenticationHandler);
}
