package com.altamiracorp.lumify.web.devTools;

import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.altamiracorp.lumify.web.WebApp;
import com.altamiracorp.lumify.web.WebAppPlugin;
import com.altamiracorp.lumify.web.devTools.user.UserAddAuthorization;
import com.altamiracorp.lumify.web.devTools.user.UserRemoveAuthorization;
import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticResourceHandler;

import javax.servlet.ServletConfig;

public class DevToolsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletConfig config, Class<? extends Handler> authenticator, AuthenticationProvider authenticatorInstance) {
        app.get("/admin/fileImport.html", authenticatorInstance, new StaticResourceHandler(getClass(), "/fileImport.html", "text/html"));
        app.get("/admin/userAdmin.html", authenticatorInstance, new StaticResourceHandler(getClass(), "/userAdmin.html", "text/html"));
        app.post("/user/auth/add", authenticator, UserAddAuthorization.class);
        app.post("/user/auth/remove", authenticator, UserRemoveAuthorization.class);
    }
}
