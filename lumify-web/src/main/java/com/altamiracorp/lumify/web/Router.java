package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.exception.LumifyAccessDeniedException;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.ServiceLoaderUtil;
import com.altamiracorp.lumify.web.routes.admin.AdminList;
import com.altamiracorp.lumify.web.routes.admin.AdminUploadOntology;
import com.altamiracorp.lumify.web.routes.artifact.*;
import com.altamiracorp.lumify.web.routes.audit.VertexAudit;
import com.altamiracorp.lumify.web.routes.config.Configuration;
import com.altamiracorp.lumify.web.routes.config.Plugin;
import com.altamiracorp.lumify.web.routes.entity.ResolveDetectedObject;
import com.altamiracorp.lumify.web.routes.entity.ResolveTermEntity;
import com.altamiracorp.lumify.web.routes.entity.UnresolveDetectedObject;
import com.altamiracorp.lumify.web.routes.entity.UnresolveTermEntity;
import com.altamiracorp.lumify.web.routes.graph.*;
import com.altamiracorp.lumify.web.routes.map.MapInitHandler;
import com.altamiracorp.lumify.web.routes.map.MapMarkerImage;
import com.altamiracorp.lumify.web.routes.map.MapTileHandler;
import com.altamiracorp.lumify.web.routes.ontology.Ontology;
import com.altamiracorp.lumify.web.routes.relationship.*;
import com.altamiracorp.lumify.web.routes.resource.ResourceGet;
import com.altamiracorp.lumify.web.routes.user.*;
import com.altamiracorp.lumify.web.routes.vertex.*;
import com.altamiracorp.lumify.web.routes.workspace.*;
import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticFileHandler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import com.google.inject.Injector;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;

public class Router extends HttpServlet {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Router.class);
    /**
     * Copied from org.eclipse.jetty.server.Request.__MULTIPART_CONFIG_ELEMENT.
     * TODO: Examine why this is necessary and how it can be abstracted to any servlet container.
     */
    private static final String JETTY_MULTIPART_CONFIG_ELEMENT = "org.eclipse.multipartConfig";
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
    private WebApp app;

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            super.init(config);

            final Injector injector = (Injector) config.getServletContext().getAttribute(Injector.class.getName());

            List<WebAppPlugin> webAppPlugins = toList(ServiceLoaderUtil.load(WebAppPlugin.class));

            app = new WebApp(config, injector);

            AuthenticationProvider authenticatorInstance = injector.getInstance(AuthenticationProvider.class);
            Class<? extends Handler> authenticator = authenticatorInstance.getClass();

            app.get("/", new StaticFileHandler(config, "/index.html"));
            app.post("/login", Login.class);
            app.post("/logout", Logout.class);

            app.get("/configuration", authenticator, Configuration.class);
            app.get("/js/configuration/plugins/*", authenticator, Plugin.class);
            app.get("/jsc/configuration/plugins/*", authenticator, Plugin.class);

            app.get("/ontology", authenticator, Ontology.class);

            app.get("/audit", authenticator, VertexAudit.class);

            app.get("/resource", authenticator, ResourceGet.class);

            app.get("/artifact/highlightedText", authenticator, ArtifactHighlightedText.class);
            app.get("/artifact/raw", authenticator, ArtifactRaw.class);
            app.get("/artifact/thumbnail", authenticator, ArtifactThumbnail.class);
            app.get("/artifact/poster-frame", authenticator, ArtifactPosterFrame.class);
            app.get("/artifact/video-preview", authenticator, ArtifactVideoPreviewImage.class);
            app.post("/artifact/import", authenticator, ArtifactImport.class);

            app.post("/entity/resolveTerm", authenticator, ResolveTermEntity.class);
            app.post("/entity/unresolveTerm", authenticator, UnresolveTermEntity.class);
            app.post("/entity/resolveDetectedObject", authenticator, ResolveDetectedObject.class);
            app.post("/entity/unresolveDetectedObject", authenticator, UnresolveDetectedObject.class);

            app.post("/vertex/property/set", authenticator, VertexSetProperty.class);
            app.post("/vertex/property/delete", authenticator, VertexDeleteProperty.class);
            app.get("/vertex/property/termMentions", authenticator, VertexGetPropertyTermMentions.class);
            app.get("/vertex/property", authenticator, VertexGetPropertyValue.class);
            app.post("/vertex/visibility/set", authenticator, VertexSetVisibility.class);
            app.get("/vertex/properties", authenticator, VertexProperties.class);
            app.get("/vertex/relationships", authenticator, VertexRelationships.class);
            app.post("/vertex/removeRelationship", authenticator, VertexRelationshipRemoval.class);
            app.post("/vertex/multiple", authenticator, VertexMultiple.class);

            app.post("/relationship/property/set", authenticator, SetRelationshipProperty.class);
            app.post("/relationship/property/delete", authenticator, DeleteRelationshipProperty.class);
            app.post("/relationship/create", authenticator, RelationshipCreate.class);
            app.get("/relationship/properties", authenticator, RelationshipProperties.class);
            app.post("/relationship/visibility/set", authenticator, RelationshipSetVisibility.class);

            app.get("/graph/findPath", authenticator, GraphFindPath.class);
            app.get("/graph/relatedVertices", authenticator, GraphRelatedVertices.class);
            app.get("/graph/vertex/search", authenticator, GraphVertexSearch.class);
            app.get("/graph/vertex/geoLocationSearch", authenticator, GraphGeoLocationSearch.class);
            app.post("/graph/vertex/uploadImage", authenticator, GraphVertexUploadImage.class);

            app.get("/workspaces", authenticator, WorkspaceList.class);
            app.post("/workspace/new", authenticator, WorkspaceNew.class);
            app.get("/workspace/diff", authenticator, WorkspaceDiff.class);
            app.get("/workspace/relationships", authenticator, WorkspaceRelationships.class);
            app.post("/workspace/relationships", authenticator, WorkspaceRelationships.class);
            app.get("/workspace/vertices", authenticator, WorkspaceVertices.class);
            app.post("/workspace/update", authenticator, WorkspaceUpdate.class);
            app.get("/workspace", authenticator, WorkspaceById.class);
            app.delete("/workspace", authenticator, WorkspaceDelete.class);
            app.post("/workspace/publish", authenticator, WorkspacePublish.class);
            app.post("/workspace/undo", authenticator, WorkspaceUndo.class);

            app.get("/user/me", authenticator, MeGet.class);
            app.get("/user", authenticator, UserList.class);
            app.get("/user/info", authenticator, UserInfo.class);

            app.get("/map/map-init.js", MapInitHandler.class);
            app.get("/map/marker/image", MapMarkerImage.class);
            app.get("/map/{z}/{x}/{y}.png", MapTileHandler.class);

            app.get("/admin", authenticator, AdminList.class);

            app.get("/admin/uploadOntology.html", authenticatorInstance, new StaticResourceHandler(getClass(), "/uploadOntology.html", "text/html"));
            app.post("/admin/uploadOntology", authenticator, AdminUploadOntology.class);

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
