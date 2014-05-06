package io.lumify.web.devTools;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.devTools.user.UserAddAuthorization;
import io.lumify.web.devTools.user.UserDelete;
import io.lumify.web.devTools.user.UserRemoveAuthorization;
import io.lumify.web.devTools.user.UserUpdatePrivileges;
import io.lumify.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletConfig;

public class DevToolsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletConfig config, Class<? extends Handler> authenticator, AuthenticationHandler authenticationHandler) {
        app.get("/admin/userAdmin.html", authenticationHandler, new StaticResourceHandler(getClass(), "/userAdmin.html", "text/html"));
        app.post("/user/auth/add", authenticator, AdminPrivilegeFilter.class, UserAddAuthorization.class);
        app.post("/user/auth/remove", authenticator, AdminPrivilegeFilter.class, UserRemoveAuthorization.class);
        app.post("/user/delete", authenticator, AdminPrivilegeFilter.class, UserDelete.class);
        app.post("/user/privileges/update", authenticator, AdminPrivilegeFilter.class, UserUpdatePrivileges.class);
    }
}
