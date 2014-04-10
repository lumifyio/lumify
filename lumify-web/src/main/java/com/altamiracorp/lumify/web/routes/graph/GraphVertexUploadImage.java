package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.ConceptType;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.*;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.artifact.ArtifactThumbnail;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;
import static com.google.common.base.Preconditions.checkNotNull;

public class GraphVertexUploadImage extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphVertexUploadImage.class);

    private static final String ATTR_GRAPH_VERTEX_ID = "graphVertexId";
    private static final String DEFAULT_MIME_TYPE = "image";
    private static final String SOURCE_UPLOAD = "User Upload";
    private static final String PROCESS = GraphVertexUploadImage.class.getName();

    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public GraphVertexUploadImage(
            final Graph graph,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, configuration);
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, ATTR_GRAPH_VERTEX_ID);
        final List<Part> files = Lists.newArrayList(request.getParts());

        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }

        final User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        final Part file = files.get(0);
        String workspaceId = getActiveWorkspaceId(request);
        Workspace workspace = this.workspaceRepository.findById(workspaceId, user);

        Vertex entityVertex = graph.getVertex(graphVertexId, authorizations);
        ElementMutation<Vertex> entityVertexMutation = entityVertex.prepareMutation();
        if (entityVertex == null) {
            LOGGER.warn("Could not find associated entity vertex for id: %s", graphVertexId);
            respondWithNotFound(response);
            return;
        }

        JSONObject visibilityJson = getLumifyVisibility(entityVertex, workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        Map<String, Object> metadata = new HashMap<String, Object>();
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setMetadata(metadata, visibilityJson);

        String title = String.format("Image of %s", LumifyProperties.TITLE.getPropertyValue(entityVertex));
        ElementBuilder<Vertex> imageVertexBuilder = convertToArtifact(file, title, visibilityJson, metadata, lumifyVisibility, authorizations);
        Vertex imageVertex = imageVertexBuilder.save();
        this.graph.flush();

        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, imageVertexBuilder, imageVertex, "", user, lumifyVisibility.getVisibility());

        // TO-DO: Create new ENTITY_IMAGE property to replace GLYPH_ICON.
        entityVertexMutation.setProperty(LumifyProperties.GLYPH_ICON.getKey(), ArtifactThumbnail.getUrl(imageVertex.getId()), metadata, lumifyVisibility.getVisibility());
        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, entityVertexMutation, entityVertex, "", user, lumifyVisibility.getVisibility());
        entityVertex = entityVertexMutation.save();
        graph.flush();

        List<Edge> existingEdges = toList(entityVertex.getEdges(imageVertex, Direction.BOTH, LabelName.ENTITY_HAS_IMAGE_RAW.toString(), authorizations));
        if (existingEdges.size() == 0) {
            EdgeBuilder edgeBuilder = graph.prepareEdge(entityVertex, imageVertex, LabelName.ENTITY_HAS_IMAGE_RAW.toString(), lumifyVisibility.getVisibility(), authorizations);
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(edgeBuilder, visibilityJson, lumifyVisibility.getVisibility());
            Edge edge = edgeBuilder.save();
            auditRepository.auditRelationship(AuditAction.CREATE, entityVertex, imageVertex, edge, "", "", user, lumifyVisibility.getVisibility());
        }

        this.workspaceRepository.updateEntityOnWorkspace(workspace, imageVertex.getId(), null, null, null, user);
        this.workspaceRepository.updateEntityOnWorkspace(workspace, entityVertex.getId(), null, null, null, user);

        this.graph.flush();
        workQueueRepository.pushUserImageQueue(imageVertex.getId().toString());

        respondWithJson(response, JsonSerializer.toJson(entityVertex, workspaceId));
    }

    private JSONObject getLumifyVisibility(Vertex entityVertex, String workspaceId) {
        JSONObject visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(entityVertex);
        if (visibilityJson == null) {
            visibilityJson = new JSONObject();
        }
        String visibilitySource = visibilityJson.optString(VisibilityTranslator.JSON_SOURCE);
        if (visibilitySource == null) {
            visibilitySource = "";
        }
        return GraphUtil.updateVisibilitySourceAndAddWorkspaceId(visibilityJson, visibilitySource, workspaceId);
    }

    private ElementBuilder<Vertex> convertToArtifact(final Part file, String title, JSONObject visibilityJson, Map<String, Object> metadata, LumifyVisibility lumifyVisibility, Authorizations authorizations) throws IOException {
        final InputStream fileInputStream = file.getInputStream();
        final byte[] rawContent = IOUtils.toByteArray(fileInputStream);
        LOGGER.debug("Uploaded file raw content byte length: %d", rawContent.length);

        final String fileName = file.getName();

        String mimeType = DEFAULT_MIME_TYPE;
        if (file.getContentType() != null) {
            mimeType = file.getContentType();
        }

        final String fileRowKey = RowKeyHelper.buildSHA256KeyString(rawContent);
        LOGGER.debug("Generated row key: %s", fileRowKey);

        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(rawContent), byte[].class);
        rawValue.searchIndex(false);
        rawValue.store(true);

        String conceptId = ontologyRepository.getConceptByIRI(ConceptType.IMAGE.toString()).getTitle();
        checkNotNull(conceptId, "Could not find image concept: " + ConceptType.IMAGE.toString());

        ElementBuilder<Vertex> vertexBuilder = graph.prepareVertex(lumifyVisibility.getVisibility(), authorizations);
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(vertexBuilder, visibilityJson, lumifyVisibility.getVisibility());
        LumifyProperties.TITLE.setProperty(vertexBuilder, title, metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.CREATE_DATE.setProperty(vertexBuilder, new Date(), metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.FILE_NAME.setProperty(vertexBuilder, fileName, metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.FILE_NAME_EXTENSION.setProperty(vertexBuilder, FilenameUtils.getExtension(fileName), metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.MIME_TYPE.setProperty(vertexBuilder, mimeType, metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.RAW.setProperty(vertexBuilder, rawValue, metadata, lumifyVisibility.getVisibility());
        OntologyLumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, conceptId, metadata, lumifyVisibility.getVisibility());
        EntityLumifyProperties.SOURCE.setProperty(vertexBuilder, SOURCE_UPLOAD, metadata, lumifyVisibility.getVisibility());
        LumifyProperties.PROCESS.setProperty(vertexBuilder, PROCESS, metadata, lumifyVisibility.getVisibility());
        return vertexBuilder;
    }
}
