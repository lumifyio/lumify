package io.lumify.web.routes.graph;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyLumifyProperties;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.EntityLumifyProperties;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.LumifyVisibilityProperties;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.*;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;
import org.securegraph.property.StreamingPropertyValue;

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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;

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
    private final String conceptIri;
    private final String entityHasImageIri;

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
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;

        this.conceptIri = configuration.get(Configuration.ONTOLOGY_IRI_ENTITY_IMAGE);
        Concept concept = ontologyRepository.getConceptByIRI(conceptIri);
        if (concept == null) {
            LOGGER.error("Could not find concept '%s' for entity upload. Configuration key %s", conceptIri, Configuration.ONTOLOGY_IRI_ENTITY_IMAGE);
        }

        this.entityHasImageIri = this.getConfiguration().get(Configuration.ONTOLOGY_IRI_ENTITY_HAS_IMAGE);
        if (this.entityHasImageIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ENTITY_HAS_IMAGE);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, ATTR_GRAPH_VERTEX_ID);
        final List<Part> files = Lists.newArrayList(request.getParts());

        Concept concept = ontologyRepository.getConceptByIRI(conceptIri);
        checkNotNull(concept, "Could not find image concept: " + conceptIri);

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
        ElementBuilder<Vertex> artifactVertexBuilder = convertToArtifact(file, title, visibilityJson, metadata, lumifyVisibility, authorizations);
        Vertex artifactVertex = artifactVertexBuilder.save(authorizations);
        this.graph.flush();

        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, artifactVertexBuilder, artifactVertex, "", user, lumifyVisibility.getVisibility());

        entityVertexMutation.setProperty(EntityLumifyProperties.IMAGE_VERTEX_ID.getPropertyName(), artifactVertex.getId(), metadata, lumifyVisibility.getVisibility());
        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, entityVertexMutation, entityVertex, "", user, lumifyVisibility.getVisibility());
        entityVertex = entityVertexMutation.save(authorizations);
        graph.flush();

        List<Edge> existingEdges = toList(entityVertex.getEdges(artifactVertex, Direction.BOTH, entityHasImageIri, authorizations));
        if (existingEdges.size() == 0) {
            EdgeBuilder edgeBuilder = graph.prepareEdge(entityVertex, artifactVertex, entityHasImageIri, lumifyVisibility.getVisibility());
            LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(edgeBuilder, visibilityJson, lumifyVisibility.getVisibility());
            Edge edge = edgeBuilder.save(authorizations);
            auditRepository.auditRelationship(AuditAction.CREATE, entityVertex, artifactVertex, edge, "", "", user, lumifyVisibility.getVisibility());
        }

        this.workspaceRepository.updateEntityOnWorkspace(workspace, artifactVertex.getId(), null, null, null, user);
        this.workspaceRepository.updateEntityOnWorkspace(workspace, entityVertex.getId(), null, null, null, user);

        graph.flush();
        workQueueRepository.pushGraphPropertyQueue(artifactVertex, null, RawLumifyProperties.RAW.getPropertyName(), workspaceId);

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

        ElementBuilder<Vertex> vertexBuilder = graph.prepareVertex(lumifyVisibility.getVisibility());
        LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.setProperty(vertexBuilder, visibilityJson, lumifyVisibility.getVisibility());
        LumifyProperties.TITLE.setProperty(vertexBuilder, title, metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.CREATE_DATE.setProperty(vertexBuilder, new Date(), metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.FILE_NAME.setProperty(vertexBuilder, fileName, metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.FILE_NAME_EXTENSION.setProperty(vertexBuilder, FilenameUtils.getExtension(fileName), metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.MIME_TYPE.setProperty(vertexBuilder, mimeType, metadata, lumifyVisibility.getVisibility());
        RawLumifyProperties.RAW.setProperty(vertexBuilder, rawValue, metadata, lumifyVisibility.getVisibility());
        OntologyLumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, conceptIri, metadata, lumifyVisibility.getVisibility());
        EntityLumifyProperties.SOURCE.setProperty(vertexBuilder, SOURCE_UPLOAD, metadata, lumifyVisibility.getVisibility());
        LumifyProperties.PROCESS.setProperty(vertexBuilder, PROCESS, metadata, lumifyVisibility.getVisibility());
        return vertexBuilder;
    }
}
