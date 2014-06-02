package io.lumify.web;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticFileHandler;
import com.altamiracorp.miniweb.StaticResourceHandler;
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

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            super.init(config);

            final Injector injector = (Injector) config.getServletContext().getAttribute(Injector.class.getName());

            app = new WebApp(config, injector);

            AuthenticationHandler authenticatorInstance = new AuthenticationHandler();
            Class<? extends Handler> authenticator = AuthenticationHandler.class;

            app.get("/", userAgentFilter, new StaticFileHandler(config, "/index.html"));
            app.post("/logout", Logout.class);

            app.get("/configuration", authenticator, Configuration.class);

            app.get("/ontology", authenticator, ReadPrivilegeFilter.class, Ontology.class);

            app.get("/audit", authenticator, ReadPrivilegeFilter.class, VertexAudit.class);

            app.get("/resource", authenticator, ReadPrivilegeFilter.class, ResourceGet.class);

            app.get("/artifact/highlightedText", authenticator, ReadPrivilegeFilter.class, ArtifactHighlightedText.class);
            app.get("/artifact/raw", authenticator, ReadPrivilegeFilter.class, ArtifactRaw.class);
            app.get("/artifact/thumbnail", authenticator, ReadPrivilegeFilter.class, ArtifactThumbnail.class);
            app.get("/artifact/poster-frame", authenticator, ReadPrivilegeFilter.class, ArtifactPosterFrame.class);
            app.get("/artifact/video-preview", authenticator, ReadPrivilegeFilter.class, ArtifactVideoPreviewImage.class);
            app.post("/artifact/import", authenticator, EditPrivilegeFilter.class, ArtifactImport.class);

            app.post("/entity/resolveTerm", authenticator, EditPrivilegeFilter.class, ResolveTermEntity.class);
            app.post("/entity/unresolveTerm", authenticator, EditPrivilegeFilter.class, UnresolveTermEntity.class);
            app.post("/entity/resolveDetectedObject", authenticator, EditPrivilegeFilter.class, ResolveDetectedObject.class);
            app.post("/entity/unresolveDetectedObject", authenticator, EditPrivilegeFilter.class, UnresolveDetectedObject.class);

            app.post("/vertex/property/set", authenticator, EditPrivilegeFilter.class, VertexSetProperty.class);
            app.post("/vertex/property/delete", authenticator, EditPrivilegeFilter.class, VertexDeleteProperty.class);
            app.get("/vertex/property/termMentions", authenticator, ReadPrivilegeFilter.class, VertexGetPropertyTermMentions.class);
            app.get("/vertex/property", authenticator, ReadPrivilegeFilter.class, VertexGetPropertyValue.class);
            app.post("/vertex/visibility/set", authenticator, EditPrivilegeFilter.class, VertexSetVisibility.class);
            app.get("/vertex/properties", authenticator, ReadPrivilegeFilter.class, VertexProperties.class);
            app.get("/vertex/relationships", authenticator, ReadPrivilegeFilter.class, VertexRelationships.class);
            app.post("/vertex/removeRelationship", authenticator, EditPrivilegeFilter.class, VertexRelationshipRemoval.class);
            app.post("/vertex/multiple", authenticator, ReadPrivilegeFilter.class, VertexMultiple.class); // this is a post method to allow large data (ie data larger than would fit in the URL)

            app.post("/relationship/property/set", authenticator, EditPrivilegeFilter.class, SetRelationshipProperty.class);
            app.post("/relationship/property/delete", authenticator, EditPrivilegeFilter.class, DeleteRelationshipProperty.class);
            app.post("/relationship/create", authenticator, EditPrivilegeFilter.class, RelationshipCreate.class);
            app.get("/relationship/properties", authenticator, ReadPrivilegeFilter.class, RelationshipProperties.class);
            app.post("/relationship/visibility/set", authenticator, EditPrivilegeFilter.class, RelationshipSetVisibility.class);

            app.get("/graph/findPath", authenticator, ReadPrivilegeFilter.class, GraphFindPath.class);
            app.get("/graph/relatedVertices", authenticator, ReadPrivilegeFilter.class, GraphRelatedVertices.class);
            app.get("/graph/vertex/search", authenticator, ReadPrivilegeFilter.class, GraphVertexSearch.class);
            app.get("/graph/vertex/geoLocationSearch", authenticator, ReadPrivilegeFilter.class, GraphGeoLocationSearch.class);
            app.post("/graph/vertex/uploadImage", authenticator, EditPrivilegeFilter.class, GraphVertexUploadImage.class);

            app.get("/workspaces", authenticator, ReadPrivilegeFilter.class, WorkspaceList.class);
            app.post("/workspace/new", authenticator, ReadPrivilegeFilter.class, WorkspaceNew.class);
            app.get("/workspace/diff", authenticator, ReadPrivilegeFilter.class, WorkspaceDiff.class);
            app.get("/workspace/relationships", authenticator, ReadPrivilegeFilter.class, WorkspaceRelationships.class);
            app.post("/workspace/relationships", authenticator, ReadPrivilegeFilter.class, WorkspaceRelationships.class); // this is a post method to allow large data (ie data larger than would fit in the URL)
            app.get("/workspace/vertices", authenticator, ReadPrivilegeFilter.class, WorkspaceVertices.class);
            app.post("/workspace/update", authenticator, ReadPrivilegeFilter.class, WorkspaceUpdate.class);
            app.get("/workspace", authenticator, ReadPrivilegeFilter.class, ReadPrivilegeFilter.class, WorkspaceById.class);
            app.delete("/workspace", authenticator, ReadPrivilegeFilter.class, WorkspaceDelete.class);
            app.post("/workspace/publish", authenticator, PublishPrivilegeFilter.class, WorkspacePublish.class);
            app.post("/workspace/undo", authenticator, EditPrivilegeFilter.class, WorkspaceUndo.class);

            app.get("/user/me", authenticator, MeGet.class);
            app.get("/user", authenticator, AdminPrivilegeFilter.class, UserGet.class);
            app.get("/users", authenticator, UserList.class);
            app.get("/user/info", authenticator, UserInfo.class);

            app.get("/map/marker/image", MapMarkerImage.class);

            app.get("/admin", authenticator, AdminPrivilegeFilter.class, AdminList.class);

            app.get("/admin/plugins", authenticator, PluginList.class);
            app.get("/admin/uploadOntology.html", authenticatorInstance, new StaticResourceHandler(getClass(), "/uploadOntology.html", "text/html"));
            app.post("/admin/uploadOntology", authenticator, AdminPrivilegeFilter.class, AdminUploadOntology.class);

            List<WebAppPlugin> webAppPlugins = toList(ServiceLoaderUtil.load(WebAppPlugin.class));
            for (WebAppPlugin webAppPlugin : webAppPlugins) {
                LOGGER.info("Loading webapp plugin: %s", webAppPlugin.getClass().getName());
                try {
                    injector.injectMembers(webAppPlugin);
                    webAppPlugin.init(app, config, authenticatorInstance);
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
