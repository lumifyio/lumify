package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.DisplayType;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.artifact.ArtifactThumbnail;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.altamiracorp.lumify.core.util.GraphUtil.toJson;

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

    @Inject
    public GraphVertexUploadImage(
            final Graph graph,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository) {
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, ATTR_GRAPH_VERTEX_ID);
        final List<Part> files = Lists.newArrayList(request.getParts());

        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }

        final User user = getUser(request);
        final Part file = files.get(0);

        Vertex entityVertex = graph.getVertex(graphVertexId, user.getAuthorizations());
        ElementMutation<Vertex> entityVertexMutation = entityVertex.prepareMutation();
        if (entityVertex == null) {
            LOGGER.warn("Could not find associated entity vertex for id: %s", graphVertexId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Visibility visibility = new Visibility("");
        ElementBuilder<Vertex> artifactBuilder = convertToArtifact(file, user);
        artifactBuilder
                .setProperty(PropertyName.DISPLAY_TYPE.toString(), DisplayType.IMAGE.toString(), visibility)
                .setProperty(PropertyName.CONCEPT_TYPE.toString(), ontologyRepository.getConceptByName(DisplayType.IMAGE.toString()).getId(), visibility)
                .setProperty(PropertyName.TITLE.toString(), "Image of " + entityVertex.getPropertyValue(PropertyName.TITLE.toString(), 0), visibility)
                .setProperty(PropertyName.SOURCE.toString(), SOURCE_UPLOAD, visibility)
                .setProperty(PropertyName.PROCESS.toString(), PROCESS, visibility);
        Vertex artifactVertex = artifactBuilder.save();

        auditRepository.auditVertexElementMutation(artifactBuilder, artifactVertex, "", user);

        entityVertexMutation.setProperty(PropertyName.GLYPH_ICON.toString(), ArtifactThumbnail.getUrl(artifactVertex.getId()), visibility);
        auditRepository.auditVertexElementMutation(entityVertexMutation, entityVertex, "", user);
        entityVertex = entityVertexMutation.save();
        graph.flush();

        Iterator<Edge> existingEdges = entityVertex.getEdges(artifactVertex, Direction.BOTH, LabelName.ENTITY_HAS_IMAGE_RAW.toString(), user.getAuthorizations()).iterator();
        if (!existingEdges.hasNext()) {
            graph.addEdge(entityVertex, artifactVertex, LabelName.ENTITY_HAS_IMAGE_RAW.toString(), visibility, user.getAuthorizations());
        }
        String labelDisplay = ontologyRepository.getDisplayNameForLabel(LabelName.ENTITY_HAS_IMAGE_RAW.toString());
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationships(AuditAction.CREATE.toString(), entityVertex, artifactVertex, labelDisplay, "", "", user);

        workQueueRepository.pushUserImageQueue(artifactVertex.getId().toString());

        respondWithJson(response, toJson(entityVertex));
    }

    private ElementBuilder<Vertex> convertToArtifact(final Part file, User user) throws IOException {
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

        Visibility visibility = new Visibility("");
        ElementBuilder<Vertex> vertexBuilder = graph.prepareVertex(visibility, user.getAuthorizations())
                .setProperty(PropertyName.CREATE_DATE.toString(), new Date(), visibility)
                .setProperty(PropertyName.FILE_NAME.toString(), fileName, visibility)
                .setProperty(PropertyName.FILE_NAME_EXTENSION.toString(), FilenameUtils.getExtension(fileName), visibility)
                .setProperty(PropertyName.MIME_TYPE.toString(), mimeType, visibility)
                .setProperty(PropertyName.RAW.toString(), rawValue, visibility);
        return vertexBuilder;
    }
}
