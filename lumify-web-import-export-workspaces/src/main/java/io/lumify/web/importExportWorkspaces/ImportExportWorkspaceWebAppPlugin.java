package io.lumify.web.importExportWorkspaces;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletConfig;

public class ImportExportWorkspaceWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletConfig config, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();

        app.get("/admin/workspaceImport.html", authenticationHandler, new StaticResourceHandler(getClass(), "/workspaceImport.html", "text/html"));
        app.get("/admin/workspaceExport.html", authenticationHandler, new StaticResourceHandler(getClass(), "/workspaceExport.html", "text/html"));
        app.get("/admin/workspace/export", authenticationHandlerClass, AdminPrivilegeFilter.class, Export.class);
        app.post("/admin/workspace/import", authenticationHandlerClass, AdminPrivilegeFilter.class, Import.class);
    }
}
