package com.altamiracorp.lumify.web;

import com.altamiracorp.miniweb.Handler;

import javax.servlet.ServletConfig;

public interface WebAppPlugin {
    void init(WebApp app, ServletConfig config, Class<? extends Handler> authenticator, AuthenticationProvider authenticatorInstance);
}
