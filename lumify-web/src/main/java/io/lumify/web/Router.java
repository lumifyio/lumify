package io.lumify.web;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticFileHandler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import com.google.inject.Injector;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ServiceLoaderUtil;
import io.lumify.web.roleFilters.AdminRoleFilter;
import io.lumify.web.roleFilters.EditRoleFilter;
import io.lumify.web.roleFilters.PublishRoleFilter;
import io.lumify.web.roleFilters.ReadRoleFilter;
import io.lumify.web.routes.admin.AdminList;
import io.lumify.web.routes.admin.AdminUploadOntology;
import io.lumify.web.routes.artifact.*;
import io.lumify.web.routes.audit.VertexAudit;
import io.lumify.web.routes.config.Configuration;
import io.lumify.web.routes.config.Plugin;
import io.lumify.web.routes.entity.ResolveDetectedObject;
import io.lumify.web.routes.entity.ResolveTermEntity;
import io.lumify.web.routes.entity.UnresolveDetectedObject;
import io.lumify.web.routes.entity.UnresolveTermEntity;
import io.lumify.web.routes.graph.*;
import io.lumify.web.routes.map.MapInitHandler;
import io.lumify.web.routes.map.MapMarkerImage;
import io.lumify.web.routes.map.MapTileHandler;
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

            List<WebAppPlugin> webAppPlugins = toList(ServiceLoaderUtil.load(WebAppPlugin.class));

            app = new WebApp(config, injector);

            AuthenticationProvider authenticatorInstance = injector.getInstance(AuthenticationProvider.class);
            Class<? extends Handler> authenticator = authenticatorInstance.getClass();

            app.get("/", userAgentFilter, new StaticFileHandler(config, "/index.html"));
            app.post("/login", Login.class);
            app.post("/logout", Logout.class);

            app.get("/configuration", authenticator, Configuration.class);
            app.get("/js/configuration/plugins/*", authenticator, Plugin.class);
            app.get("/jsc/configuration/plugins/*", authenticator, Plugin.class);

            app.get("/ontology", authenticator, ReadRoleFilter.class, Ontology.class);

            app.get("/audit", authenticator, ReadRoleFilter.class, VertexAudit.class);

            app.get("/resource", authenticator, ReadRoleFilter.class, ResourceGet.class);

            app.get("/artifact/highlightedText", authenticator, ReadRoleFilter.class, ArtifactHighlightedText.class);
            app.get("/artifact/raw", authenticator, ReadRoleFilter.class, ArtifactRaw.class);
            app.get("/artifact/thumbnail", authenticator, ReadRoleFilter.class, ArtifactThumbnail.class);
            app.get("/artifact/poster-frame", authenticator, ReadRoleFilter.class, ArtifactPosterFrame.class);
            app.get("/artifact/video-preview", authenticator, ReadRoleFilter.class, ArtifactVideoPreviewImage.class);
            app.post("/artifact/import", authenticator, EditRoleFilter.class, ArtifactImport.class);

            app.post("/entity/resolveTerm", authenticator, EditRoleFilter.class, ResolveTermEntity.class);
            app.post("/entity/unresolveTerm", authenticator, EditRoleFilter.class, UnresolveTermEntity.class);
            app.post("/entity/resolveDetectedObject", authenticator, EditRoleFilter.class, ResolveDetectedObject.class);
            app.post("/entity/unresolveDetectedObject", authenticator, EditRoleFilter.class, UnresolveDetectedObject.class);

            app.post("/vertex/property/set", authenticator, EditRoleFilter.class, VertexSetProperty.class);
            app.post("/vertex/property/delete", authenticator, EditRoleFilter.class, VertexDeleteProperty.class);
            app.get("/vertex/property/termMentions", authenticator, ReadRoleFilter.class, VertexGetPropertyTermMentions.class);
            app.get("/vertex/property", authenticator, ReadRoleFilter.class, VertexGetPropertyValue.class);
            app.post("/vertex/visibility/set", authenticator, EditRoleFilter.class, VertexSetVisibility.class);
            app.get("/vertex/properties", authenticator, ReadRoleFilter.class, VertexProperties.class);
            app.get("/vertex/relationships", authenticator, ReadRoleFilter.class, VertexRelationships.class);
            app.post("/vertex/removeRelationship", authenticator, EditRoleFilter.class, VertexRelationshipRemoval.class);
            app.post("/vertex/multiple", authenticator, ReadRoleFilter.class, VertexMultiple.class); // this is a post method to allow large data (ie data larger than would fit in the URL)

            app.post("/relationship/property/set", authenticator, EditRoleFilter.class, SetRelationshipProperty.class);
            app.post("/relationship/property/delete", authenticator, EditRoleFilter.class, DeleteRelationshipProperty.class);
            app.post("/relationship/create", authenticator, EditRoleFilter.class, RelationshipCreate.class);
            app.get("/relationship/properties", authenticator, ReadRoleFilter.class, RelationshipProperties.class);
            app.post("/relationship/visibility/set", authenticator, EditRoleFilter.class, RelationshipSetVisibility.class);

            app.get("/graph/findPath", authenticator, ReadRoleFilter.class, GraphFindPath.class);
            app.get("/graph/relatedVertices", authenticator, ReadRoleFilter.class, GraphRelatedVertices.class);
            app.get("/graph/vertex/search", authenticator, ReadRoleFilter.class, GraphVertexSearch.class);
            app.get("/graph/vertex/geoLocationSearch", authenticator, ReadRoleFilter.class, GraphGeoLocationSearch.class);
            app.post("/graph/vertex/uploadImage", authenticator, EditRoleFilter.class, GraphVertexUploadImage.class);

            app.get("/workspaces", authenticator, ReadRoleFilter.class, WorkspaceList.class);
            app.post("/workspace/new", authenticator, ReadRoleFilter.class, WorkspaceNew.class);
            app.get("/workspace/diff", authenticator, ReadRoleFilter.class, WorkspaceDiff.class);
            app.get("/workspace/relationships", authenticator, ReadRoleFilter.class, WorkspaceRelationships.class);
            app.post("/workspace/relationships", authenticator, ReadRoleFilter.class, WorkspaceRelationships.class); // this is a post method to allow large data (ie data larger than would fit in the URL)
            app.get("/workspace/vertices", authenticator, ReadRoleFilter.class, WorkspaceVertices.class);
            app.post("/workspace/update", authenticator, ReadRoleFilter.class, WorkspaceUpdate.class);
            app.get("/workspace", authenticator, ReadRoleFilter.class, ReadRoleFilter.class, WorkspaceById.class);
            app.delete("/workspace", authenticator, ReadRoleFilter.class, WorkspaceDelete.class);
            app.post("/workspace/publish", authenticator, PublishRoleFilter.class, WorkspacePublish.class);
            app.post("/workspace/undo", authenticator, EditRoleFilter.class, WorkspaceUndo.class);

            app.get("/user/me", authenticator, MeGet.class);
            app.get("/user", authenticator, UserList.class);
            app.get("/user/info", authenticator, UserInfo.class);

            app.get("/map/map-init.js", MapInitHandler.class);
            app.get("/map/marker/image", MapMarkerImage.class);
            app.get("/map/{z}/{x}/{y}.png", MapTileHandler.class);

            app.get("/admin", authenticator, AdminRoleFilter.class, AdminList.class);

            app.get("/admin/uploadOntology.html", authenticatorInstance, new StaticResourceHandler(getClass(), "/uploadOntology.html", "text/html"));
            app.post("/admin/uploadOntology", authenticator, AdminRoleFilter.class, AdminUploadOntology.class);

            for (WebAppPlugin webAppPlugin : webAppPlugins) {
                LOGGER.info("Loading webAppPlugin: %s", webAppPlugin.getClass().getName());
                webAppPlugin.init(app, config, authenticator, authenticatorInstance);
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
