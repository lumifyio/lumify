package io.lumify.web.devTools;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.handlers.StaticResourceHandler;
import io.lumify.web.LumifyCsrfHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.devTools.ontology.SaveOntologyConcept;
import io.lumify.web.devTools.user.*;
import io.lumify.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

public class DevToolsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = LumifyCsrfHandler.class;

        app.get("/jsc/io/lumify/web/devTools/less/vertex-editor.less",
                new StaticResourceHandler(getClass(), "/io/lumify/web/devTools/less/vertex-editor.less", "text/less"));
        app.get("/jsc/io/lumify/web/devTools/templates/vertex-editor.hbs",
                new StaticResourceHandler(getClass(), "/io/lumify/web/devTools/templates/vertex-editor.hbs", "text/html"));
        app.registerJavaScript("/io/lumify/web/devTools/vertex-editor-plugin.js");
        app.registerResourceBundle("/io/lumify/web/devTools/messages.properties");

        app.get("/jsc/io/lumify/web/devTools/templates/requeue.hbs",
                new StaticResourceHandler(getClass(), "/io/lumify/web/devTools/templates/requeue.hbs", "text/html"));
        app.registerJavaScript("/io/lumify/web/devTools/requeue-plugin.js");

        app.get("/admin/userAdmin.html", authenticationHandler, new StaticResourceHandler(getClass(), "/userAdmin.html", "text/html"));
        app.post("/user/auth/add", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserAddAuthorization.class);
        app.post("/user/auth/remove", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserRemoveAuthorization.class);
        app.post("/user/delete", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserDelete.class);
        app.post("/user/privileges/update", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserUpdatePrivileges.class);

        app.post("/workspace/shareWithMe", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, WorkspaceShareWithMe.class);

        app.get("/admin/graphVertexEditor.html", authenticationHandler, new StaticResourceHandler(getClass(), "/graphVertexEditor.html", "text/html"));

        app.get("/admin/requeue.html", authenticationHandler, new StaticResourceHandler(getClass(), "/requeue.html", "text/html"));
        app.post("/admin/queueVertices", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, QueueVertices.class);
        app.post("/admin/queueEdges", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, QueueEdges.class);

        app.get("/admin/editOntology.html", authenticationHandler, new StaticResourceHandler(getClass(), "/editOntology.html", "text/html"));
        app.post("/admin/saveOntologyConcept", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, SaveOntologyConcept.class);

        app.registerJavaScript("/io/lumify/web/devTools/dev.js");
        app.registerCss("/io/lumify/web/devTools/dev.css");
    }
}
