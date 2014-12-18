package io.lumify.web;

import com.google.inject.Injector;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ServiceLoaderUtil;
import io.lumify.miniweb.Handler;
import io.lumify.web.privilegeFilters.*;
import io.lumify.web.routes.Index;
import io.lumify.web.routes.admin.AdminList;
import io.lumify.web.routes.admin.AdminUploadOntology;
import io.lumify.web.routes.admin.PluginList;
import io.lumify.web.routes.config.Configuration;
import io.lumify.web.routes.edge.*;
import io.lumify.web.routes.longRunningProcess.LongRunningProcessById;
import io.lumify.web.routes.longRunningProcess.LongRunningProcessCancel;
import io.lumify.web.routes.longRunningProcess.LongRunningProcessDelete;
import io.lumify.web.routes.notification.Notifications;
import io.lumify.web.routes.notification.SystemNotificationDelete;
import io.lumify.web.routes.notification.SystemNotificationSave;
import io.lumify.web.routes.notification.UserNotificationMarkRead;
import io.lumify.web.routes.ontology.Ontology;
import io.lumify.web.routes.resource.MapMarkerImage;
import io.lumify.web.routes.resource.ResourceGet;
import io.lumify.web.routes.user.*;
import io.lumify.web.routes.vertex.*;
import io.lumify.web.routes.workspace.*;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

public class Router extends HttpServlet {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Router.class);

    /**
     * Copied from org.eclipse.jetty.server.Request.__MULTIPART_CONFIG_ELEMENT.
     * TODO: Examine why this is necessary and how it can be abstracted to any servlet container.
     */
    private static final String JETTY_MULTIPART_CONFIG_ELEMENT = "org.eclipse.multipartConfig";
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
    private WebApp app;

    public Router(ServletContext servletContext) {
        try {
            final Injector injector = (Injector) servletContext.getAttribute(Injector.class.getName());

            app = new WebApp(servletContext, injector);

            AuthenticationHandler authenticatorInstance = new AuthenticationHandler();
            Class<? extends Handler> authenticator = AuthenticationHandler.class;

            Class<? extends Handler> csrfProtector = LumifyCsrfHandler.class;

            app.get("/", UserAgentFilter.class, csrfProtector, Index.class);
            app.get("/configuration", csrfProtector, Configuration.class);
            app.post("/logout", csrfProtector, Logout.class);

            app.get("/ontology", authenticator, csrfProtector, ReadPrivilegeFilter.class, Ontology.class);

            app.get("/notification/all", authenticator, csrfProtector, ReadPrivilegeFilter.class, Notifications.class);
            app.post("/notification/mark-read", authenticator, csrfProtector, ReadPrivilegeFilter.class, UserNotificationMarkRead.class);
            app.post("/notification/system", authenticator, csrfProtector, AdminPrivilegeFilter.class, SystemNotificationSave.class);
            app.delete("/notification/system", authenticator, csrfProtector, AdminPrivilegeFilter.class, SystemNotificationDelete.class);

            app.get("/resource", authenticator, csrfProtector, ReadPrivilegeFilter.class, ResourceGet.class);
            app.get("/map/marker/image", csrfProtector, MapMarkerImage.class);  // TODO combine with /resource

            app.get("/vertex/highlighted-text", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexHighlightedText.class);
            app.get("/vertex/raw", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexRaw.class);
            app.get("/vertex/thumbnail", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexThumbnail.class);
            app.get("/vertex/poster-frame", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexPosterFrame.class);
            app.get("/vertex/video-preview", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexVideoPreviewImage.class);
            app.post("/vertex/import", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexImport.class);
            app.post("/vertex/resolve-term", authenticator, csrfProtector, EditPrivilegeFilter.class, ResolveTermEntity.class);
            app.post("/vertex/unresolve-term", authenticator, csrfProtector, EditPrivilegeFilter.class, UnresolveTermEntity.class);
            app.post("/vertex/resolve-detected-object", authenticator, csrfProtector, EditPrivilegeFilter.class, ResolveDetectedObject.class);
            app.post("/vertex/unresolve-detected-object", authenticator, csrfProtector, EditPrivilegeFilter.class, UnresolveDetectedObject.class);
            app.get("/vertex/detected-objects", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetDetectedObjects.class);
            app.get("/vertex/property", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetPropertyValue.class);
            app.post("/vertex/property", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexSetProperty.class);
            app.post("/vertex/comment", authenticator, csrfProtector, CommentPrivilegeFilter.class, VertexSetProperty.class);
            app.delete("/vertex/property", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexDeleteProperty.class);
            app.get("/vertex/term-mentions", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetTermMentions.class);
            app.post("/vertex/visibility", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexSetVisibility.class);
            app.get("/vertex/properties", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexProperties.class);
            app.get("/vertex/edges", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexEdges.class);
            app.post("/vertex/multiple", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexMultiple.class); // this is a post method to allow large data (ie data larger than would fit in the URL)
            app.post("/vertex/new", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexNew.class);
            app.get("/vertex/search", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexSearch.class);
            app.get("/vertex/geo-search", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGeoSearch.class);
            app.post("/vertex/upload-image", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexUploadImage.class);
            app.get("/vertex/find-path", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexFindPath.class);
            app.get("/vertex/find-related", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexFindRelated.class);
            app.get("/vertex/audit", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexAudit.class);

            app.post("/edge/property", authenticator, csrfProtector, EditPrivilegeFilter.class, SetEdgeProperty.class);
            app.post("/edge/comment", authenticator, csrfProtector, CommentPrivilegeFilter.class, SetEdgeProperty.class);
            app.delete("/edge", authenticator, csrfProtector, EditPrivilegeFilter.class, EdgeDelete.class);
            app.delete("/edge/property", authenticator, csrfProtector, EditPrivilegeFilter.class, DeleteEdgeProperty.class);
            app.post("/edge/create", authenticator, csrfProtector, EditPrivilegeFilter.class, EdgeCreate.class);
            app.get("/edge/properties", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeProperties.class);
            app.post("/edge/visibility", authenticator, csrfProtector, EditPrivilegeFilter.class, EdgeSetVisibility.class);
            app.get("/edge/audit", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeAudit.class);

            app.get("/workspace/all", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceList.class);
            app.post("/workspace/create", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceCreate.class);
            app.get("/workspace/diff", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceDiff.class);
            app.get("/workspace/edges", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceEdges.class);
            app.post("/workspace/edges", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceEdges.class); // this is a post method to allow large data (ie data larger than would fit in the URL)
            app.get("/workspace/vertices", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceVertices.class);
            app.post("/workspace/update", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceUpdate.class);
            app.get("/workspace", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceById.class);
            app.delete("/workspace", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceDelete.class);
            app.post("/workspace/publish", authenticator, csrfProtector, PublishPrivilegeFilter.class, WorkspacePublish.class);
            app.post("/workspace/undo", authenticator, csrfProtector, EditPrivilegeFilter.class, WorkspaceUndo.class);

            app.get("/user/me", authenticator, csrfProtector, MeGet.class);
            app.post("/user/ui-preferences", authenticator, csrfProtector, UserSetUiPreferences.class);
            app.get("/user/all", authenticator, csrfProtector, UserList.class);
            app.post("/user/all", authenticator, csrfProtector, UserList.class);
            app.get("/user", authenticator, csrfProtector, AdminPrivilegeFilter.class, UserGet.class);

            app.get("/long-running-process", authenticator, csrfProtector, LongRunningProcessById.class);
            app.delete("/long-running-process", authenticator, csrfProtector, LongRunningProcessDelete.class);
            app.post("/long-running-process/cancel", authenticator, csrfProtector, LongRunningProcessCancel.class);

            app.get("/admin/all", authenticator, csrfProtector, AdminPrivilegeFilter.class, AdminList.class);
            app.get("/admin/plugins", authenticator, csrfProtector, PluginList.class);
            app.post("/admin/upload-ontology", authenticator, csrfProtector, AdminPrivilegeFilter.class, AdminUploadOntology.class);

            List<WebAppPlugin> webAppPlugins = toList(ServiceLoaderUtil.load(WebAppPlugin.class));
            for (WebAppPlugin webAppPlugin : webAppPlugins) {
                LOGGER.info("Loading webapp plugin: %s", webAppPlugin.getClass().getName());
                try {
                    injector.injectMembers(webAppPlugin);
                    webAppPlugin.init(app, servletContext, authenticatorInstance);
                } catch (Exception e) {
                    throw new LumifyException("Could not initialize webapp plugin: " + webAppPlugin.getClass().getName(), e);
                }
            }

            app.onException(LumifyAccessDeniedException.class, new ErrorCodeHandler(HttpServletResponse.SC_FORBIDDEN));
        } catch (Exception ex) {
            LOGGER.error("Failed to initialize Router", ex);
            throw new RuntimeException("Failed to initialize " + getClass().getName(), ex);
        }
    }

    @Override
    public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        try {
            if (req.getContentType() != null && req.getContentType().startsWith("multipart/form-data")) {
                req.setAttribute(JETTY_MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
            }

            HttpServletResponse httpResponse = (HttpServletResponse) resp;
            httpResponse.addHeader("Accept-Ranges", "bytes");
            app.handle((HttpServletRequest) req, httpResponse);
        } catch (ConnectionClosedException cce) {
            LOGGER.debug("Connection closed by client", cce);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
