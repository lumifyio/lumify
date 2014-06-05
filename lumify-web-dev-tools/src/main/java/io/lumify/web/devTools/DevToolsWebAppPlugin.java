package io.lumify.web.devTools;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import io.lumify.web.LumifyCsrfHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.devTools.user.*;
import io.lumify.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletConfig;

public class DevToolsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletConfig config, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = LumifyCsrfHandler.class;

        app.get("/admin/userAdmin.html", authenticationHandler, new StaticResourceHandler(getClass(), "/userAdmin.html", "text/html"));
        app.post("/user/auth/add", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserAddAuthorization.class);
        app.post("/user/auth/remove", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserRemoveAuthorization.class);
        app.post("/user/delete", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserDelete.class);
        app.post("/user/privileges/update", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserUpdatePrivileges.class);

        app.post("/workspace/shareWithMe", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, WorkspaceShareWithMe.class);

        app.get("/admin/requeue.html", authenticationHandler, new StaticResourceHandler(getClass(), "/requeue.html", "text/html"));
        app.post("/admin/queueVertices", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, QueueVertices.class);
        app.post("/admin/queueEdges", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, QueueEdges.class);

        app.registerJavaScript("/io/lumify/web/devTools/dev.js");
        app.registerCss("/io/lumify/web/devTools/dev.css");
    }
}
