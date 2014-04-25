package io.lumify.web.devTools;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import io.lumify.web.AuthenticationProvider;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.devTools.user.UserAddAuthorization;
import io.lumify.web.devTools.user.UserRemoveAuthorization;
import io.lumify.web.roleFilters.AdminRoleFilter;

import javax.servlet.ServletConfig;

public class DevToolsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletConfig config, Class<? extends Handler> authenticator, AuthenticationProvider authenticatorInstance) {
        app.get("/admin/userAdmin.html", authenticatorInstance, new StaticResourceHandler(getClass(), "/userAdmin.html", "text/html"));
        app.post("/user/auth/add", authenticator, AdminRoleFilter.class, UserAddAuthorization.class);
        app.post("/user/auth/remove", authenticator, AdminRoleFilter.class, UserRemoveAuthorization.class);
    }
}
