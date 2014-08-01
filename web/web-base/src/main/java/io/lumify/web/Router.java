package io.lumify.web;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.handlers.StaticFileHandler;
import com.altamiracorp.miniweb.handlers.StaticResourceHandler;
import com.google.inject.Injector;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ServiceLoaderUtil;
import io.lumify.web.privilegeFilters.AdminPrivilegeFilter;
import io.lumify.web.privilegeFilters.EditPrivilegeFilter;
import io.lumify.web.privilegeFilters.PublishPrivilegeFilter;
import io.lumify.web.privilegeFilters.ReadPrivilegeFilter;
import io.lumify.web.routes.admin.AdminList;
import io.lumify.web.routes.admin.AdminUploadOntology;
import io.lumify.web.routes.admin.PluginList;
import io.lumify.web.routes.artifact.*;
import io.lumify.web.routes.audit.VertexAudit;
import io.lumify.web.routes.config.Configuration;
import io.lumify.web.routes.entity.ResolveDetectedObject;
import io.lumify.web.routes.entity.ResolveTermEntity;
import io.lumify.web.routes.entity.UnresolveDetectedObject;
import io.lumify.web.routes.entity.UnresolveTermEntity;
import io.lumify.web.routes.graph.*;
import io.lumify.web.routes.map.MapMarkerImage;
import io.lumify.web.routes.ontology.Ontology;
import io.lumify.web.routes.plugins.PluginsCss;
import io.lumify.web.routes.plugins.PluginsJavaScript;
import io.lumify.web.routes.relationship.*;
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
    private UserAgentFilter userAgentFilter = new UserAgentFilter();

    public Router(ServletContext servletContext) {
        try {
            final Injector injector = (Injector) servletContext.getAttribute(Injector.class.getName());

            app = new WebApp(servletContext, injector);

            AuthenticationHandler authenticatorInstance = new AuthenticationHandler();
            Class<? extends Handler> authenticator = AuthenticationHandler.class;

            Class<? extends Handler> csrfProtector = LumifyCsrfHandler.class;

            app.get("/", userAgentFilter, new StaticFileHandler(servletContext, "/index.html"));
            app.get("/plugins.css", csrfProtector, PluginsCss.class);
            app.get("/plugins.js", csrfProtector, PluginsJavaScript.class);
            app.get("/configuration", csrfProtector, Configuration.class);
            app.post("/logout", csrfProtector, Logout.class);

            app.get("/ontology", authenticator, csrfProtector, ReadPrivilegeFilter.class, Ontology.class);

            app.get("/audit", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexAudit.class);

            app.get("/resource", authenticator, csrfProtector, ReadPrivilegeFilter.class, ResourceGet.class);

            app.get("/artifact/highlightedText", authenticator, csrfProtector, ReadPrivilegeFilter.class, ArtifactHighlightedText.class);
            app.get("/artifact/raw", authenticator, csrfProtector, ReadPrivilegeFilter.class, ArtifactRaw.class);
            app.get("/artifact/thumbnail", authenticator, csrfProtector, ReadPrivilegeFilter.class, ArtifactThumbnail.class);
            app.get("/artifact/poster-frame", authenticator, csrfProtector, ReadPrivilegeFilter.class, ArtifactPosterFrame.class);
            app.get("/artifact/video-preview", authenticator, csrfProtector, ReadPrivilegeFilter.class, ArtifactVideoPreviewImage.class);
            app.post("/artifact/import", authenticator, csrfProtector, EditPrivilegeFilter.class, ArtifactImport.class);

            app.post("/entity/resolveTerm", authenticator, csrfProtector, EditPrivilegeFilter.class, ResolveTermEntity.class);
            app.post("/entity/unresolveTerm", authenticator, csrfProtector, EditPrivilegeFilter.class, UnresolveTermEntity.class);
            app.post("/entity/resolveDetectedObject", authenticator, csrfProtector, EditPrivilegeFilter.class, ResolveDetectedObject.class);
            app.post("/entity/unresolveDetectedObject", authenticator, csrfProtector, EditPrivilegeFilter.class, UnresolveDetectedObject.class);

            app.post("/vertex/property/set", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexSetProperty.class);
            app.post("/vertex/property/delete", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexDeleteProperty.class);
            app.get("/vertex/property/termMentions", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetPropertyTermMentions.class);
            app.get("/vertex/property", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetPropertyValue.class);
            app.post("/vertex/visibility/set", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexSetVisibility.class);
            app.get("/vertex/properties", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexProperties.class);
            app.get("/vertex/relationships", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexRelationships.class);
            app.post("/vertex/removeRelationship", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexRelationshipRemoval.class);
            app.post("/vertex/multiple", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexMultiple.class); // this is a post method to allow large data (ie data larger than would fit in the URL)

            app.post("/relationship/property/set", authenticator, csrfProtector, EditPrivilegeFilter.class, SetRelationshipProperty.class);
            app.post("/relationship/property/delete", authenticator, csrfProtector, EditPrivilegeFilter.class, DeleteRelationshipProperty.class);
            app.post("/relationship/create", authenticator, csrfProtector, EditPrivilegeFilter.class, RelationshipCreate.class);
            app.get("/relationship/properties", authenticator, csrfProtector, ReadPrivilegeFilter.class, RelationshipProperties.class);
            app.post("/relationship/visibility/set", authenticator, csrfProtector, EditPrivilegeFilter.class, RelationshipSetVisibility.class);

            app.get("/graph/findPath", authenticator, csrfProtector, ReadPrivilegeFilter.class, GraphFindPath.class);
            app.get("/graph/relatedVertices", authenticator, csrfProtector, ReadPrivilegeFilter.class, GraphRelatedVertices.class);
            app.get("/graph/vertex/search", authenticator, csrfProtector, ReadPrivilegeFilter.class, GraphVertexSearch.class);
            app.get("/graph/vertex/geoLocationSearch", authenticator, csrfProtector, ReadPrivilegeFilter.class, GraphGeoLocationSearch.class);
            app.post("/graph/vertex/uploadImage", authenticator, csrfProtector, EditPrivilegeFilter.class, GraphVertexUploadImage.class);

            app.get("/workspaces", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceList.class);
            app.post("/workspace/new", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceNew.class);
            app.get("/workspace/diff", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceDiff.class);
            app.get("/workspace/relationships", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceRelationships.class);
            app.post("/workspace/relationships", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceRelationships.class); // this is a post method to allow large data (ie data larger than would fit in the URL)
            app.get("/workspace/vertices", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceVertices.class);
            app.post("/workspace/update", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceUpdate.class);
            app.get("/workspace", authenticator, csrfProtector, ReadPrivilegeFilter.class, ReadPrivilegeFilter.class, WorkspaceById.class);
            app.delete("/workspace", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceDelete.class);
            app.post("/workspace/publish", authenticator, csrfProtector, PublishPrivilegeFilter.class, WorkspacePublish.class);
            app.post("/workspace/undo", authenticator, csrfProtector, EditPrivilegeFilter.class, WorkspaceUndo.class);

            app.get("/user/me", authenticator, csrfProtector, MeGet.class);
            app.get("/user/ui-preferences/set", authenticator, csrfProtector, UserSetUiPreferences.class);
            app.get("/user", authenticator, csrfProtector, AdminPrivilegeFilter.class, UserGet.class);
            app.get("/users", authenticator, csrfProtector, UserList.class);
            app.get("/user/info", authenticator, csrfProtector, UserInfo.class);

            app.get("/map/marker/image", csrfProtector, MapMarkerImage.class);

            app.get("/admin", authenticator, csrfProtector, AdminPrivilegeFilter.class, AdminList.class);

            app.get("/admin/plugins", authenticator, csrfProtector, PluginList.class);
            app.get("/admin/uploadOntology.html", authenticatorInstance, new StaticResourceHandler(getClass(), "/uploadOntology.html", "text/html"));
            app.post("/admin/uploadOntology", authenticator, csrfProtector, AdminPrivilegeFilter.class, AdminUploadOntology.class);

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
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
