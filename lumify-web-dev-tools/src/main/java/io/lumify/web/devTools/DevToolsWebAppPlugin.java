package io.lumify.web.devTools;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticResourceHandler;
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
    public void init(WebApp app, ServletConfig config, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();

        app.get("/admin/userAdmin.html", authenticationHandler, new StaticResourceHandler(getClass(), "/userAdmin.html", "text/html"));
        app.post("/user/auth/add", authenticationHandlerClass, AdminPrivilegeFilter.class, UserAddAuthorization.class);
        app.post("/user/auth/remove", authenticationHandlerClass, AdminPrivilegeFilter.class, UserRemoveAuthorization.class);
        app.post("/user/delete", authenticationHandlerClass, AdminPrivilegeFilter.class, UserDelete.class);
        app.post("/user/privileges/update", authenticationHandlerClass, AdminPrivilegeFilter.class, UserUpdatePrivileges.class);

        app.get("/admin/requeue.html", authenticationHandler, new StaticResourceHandler(getClass(), "/requeue.html", "text/html"));
        app.post("/admin/queueVertices", authenticationHandlerClass, AdminPrivilegeFilter.class, QueueVertices.class);
        app.post("/admin/queueEdges", authenticationHandlerClass, AdminPrivilegeFilter.class, QueueEdges.class);
        app.post("/admin/reindex", authenticationHandlerClass, AdminPrivilegeFilter.class, ReIndexAll.class);
    }
}
