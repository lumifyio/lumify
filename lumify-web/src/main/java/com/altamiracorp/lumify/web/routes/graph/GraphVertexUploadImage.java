package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactMetadata;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
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
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
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

    private final ArtifactRepository artifactRepository;
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public GraphVertexUploadImage(
            final ArtifactRepository artifactRepository,
            final Graph graph,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository) {
        this.artifactRepository = artifactRepository;
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

        final Vertex entityVertex = graph.getVertex(graphVertexId, user.getAuthorizations());
        if (entityVertex == null) {
            LOGGER.warn("Could not find associated entity vertex for id: %s", graphVertexId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Artifact artifact = convertToArtifact(file);
        artifactRepository.save(artifact, user.getModelUserContext());

        ArtifactExtractedInfo artifactDetails = new ArtifactExtractedInfo();
        artifactDetails.setConceptType(DisplayType.IMAGE.toString());
        artifactDetails.setTitle("Image of " + entityVertex.getPropertyValue(PropertyName.TITLE.toString(), 0));
        artifactDetails.setSource(SOURCE_UPLOAD);
        artifactDetails.setProcess(PROCESS);

        Vertex artifactVertex = null;
        if (artifact.getMetadata().getGraphVertexId() != null) {
            artifactVertex = graph.getVertex(artifact.getMetadata().getGraphVertexId(), user.getAuthorizations());
        }
        if (artifactVertex == null) {
            artifactVertex = artifactRepository.saveToGraph(artifact, artifactDetails, user);
        }

        entityVertex.setProperty(PropertyName.GLYPH_ICON.toString(), ArtifactThumbnail.getUrl(artifactVertex.getId()), new Visibility(""));
        graph.flush();

        // TODO: replace second"" when we implement commenting on ui
        auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), entityVertex, PropertyName.GLYPH_ICON.toString(), "", "", user);

        Iterator<Edge> existingEdges = entityVertex.getEdges(artifactVertex, Direction.BOTH, LabelName.ENTITY_HAS_IMAGE_RAW.toString(), user.getAuthorizations()).iterator();
        if (!existingEdges.hasNext()) {
            graph.addEdge(entityVertex, artifactVertex, LabelName.ENTITY_HAS_IMAGE_RAW.toString(), new Visibility(""));
        }
        String labelDisplay = ontologyRepository.getDisplayNameForLabel(LabelName.ENTITY_HAS_IMAGE_RAW.toString());
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationships(AuditAction.CREATE.toString(), entityVertex, artifactVertex, labelDisplay, "", "", user);

        workQueueRepository.pushUserImageQueue(artifactVertex.getId().toString());

        respondWithJson(response, toJson(entityVertex));
    }

    private Artifact convertToArtifact(final Part file) throws IOException {
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

        Artifact artifact = new Artifact(fileRowKey);
        ArtifactMetadata metadata = artifact.getMetadata();
        metadata.setCreateDate(new Date());
        metadata.setRaw(rawContent);
        metadata.setFileName(fileName);
        metadata.setFileExtension(FilenameUtils.getExtension(fileName));
        metadata.setMimeType(mimeType);
        metadata.setHighlightedText("");

        return artifact;
    }
}
