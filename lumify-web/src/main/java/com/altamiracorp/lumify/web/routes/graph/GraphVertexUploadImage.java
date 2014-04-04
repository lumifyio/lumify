package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.ConceptType;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.DISPLAY_TYPE;
import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.SOURCE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.GLYPH_ICON;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;
import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.*;
import static com.altamiracorp.lumify.core.util.GraphUtil.toJson;

public class GraphVertexUploadImage extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphVertexUploadImage.class);

    private static final String ATTR_GRAPH_VERTEX_ID = "graphVertexId";
    private static final String DEFAULT_MIME_TYPE = "image";
    private static final String SOURCE_UPLOAD = "User Upload";
    private static final String PROCESS = GraphVertexUploadImage.class.getName();
    private final LumifyVisibility lumifyVisibility = new LumifyVisibility();

    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public GraphVertexUploadImage(
            final Graph graph,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final WorkQueueRepository workQueueRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
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
        Authorizations authorizations = getAuthorizations(request, user);
        final Part file = files.get(0);
        String workspaceId = getWorkspaceId(request);

        Vertex entityVertex = graph.getVertex(graphVertexId, authorizations);
        ElementMutation<Vertex> entityVertexMutation = entityVertex.prepareMutation();
        if (entityVertex == null) {
            LOGGER.warn("Could not find associated entity vertex for id: %s", graphVertexId);
            respondWithNotFound(response);
            return;
        }

        ElementBuilder<Vertex> artifactBuilder = convertToArtifact(file, authorizations);
        String conceptId = ontologyRepository.getConceptByIRI(ConceptType.IMAGE.toString()).getId();
        DISPLAY_TYPE.setProperty(artifactBuilder, ConceptType.IMAGE.toString(), lumifyVisibility.getVisibility());
        CONCEPT_TYPE.setProperty(artifactBuilder, conceptId, lumifyVisibility.getVisibility());
        TITLE.setProperty(artifactBuilder, String.format("Image of %s", TITLE.getPropertyValue(entityVertex)), lumifyVisibility.getVisibility());
        SOURCE.setProperty(artifactBuilder, SOURCE_UPLOAD, lumifyVisibility.getVisibility());
        LumifyProperties.PROCESS.setProperty(artifactBuilder, PROCESS, lumifyVisibility.getVisibility());
        Vertex artifactVertex = artifactBuilder.save();

        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, artifactBuilder, artifactVertex, "", user, lumifyVisibility.getVisibility());

        // TO-DO: Create new ENTITY_IMAGE property to replace GLYPH_ICON.
        entityVertexMutation.setProperty(GLYPH_ICON.getKey(), ArtifactThumbnail.getUrl(artifactVertex.getId()), lumifyVisibility.getVisibility());
        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, entityVertexMutation, entityVertex, "", user, lumifyVisibility.getVisibility());
        entityVertex = entityVertexMutation.save();
        graph.flush();

        Iterator<Edge> existingEdges = entityVertex.getEdges(artifactVertex, Direction.BOTH, LabelName.ENTITY_HAS_IMAGE_RAW.toString(), authorizations).iterator();
        if (!existingEdges.hasNext()) {
            Edge edge = graph.addEdge(entityVertex, artifactVertex, LabelName.ENTITY_HAS_IMAGE_RAW.toString(), lumifyVisibility.getVisibility(), authorizations);
            auditRepository.auditRelationship(AuditAction.CREATE, entityVertex, artifactVertex, edge, "", "", user, lumifyVisibility.getVisibility());
        }

        workQueueRepository.pushUserImageQueue(artifactVertex.getId().toString());

        respondWithJson(response, toJson(entityVertex, workspaceId));
    }

    private ElementBuilder<Vertex> convertToArtifact(final Part file, Authorizations authorizations) throws IOException {
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

        ElementBuilder<Vertex> vertexBuilder = graph.prepareVertex(lumifyVisibility.getVisibility(), authorizations);
        CREATE_DATE.setProperty(vertexBuilder, new Date(), lumifyVisibility.getVisibility());
        FILE_NAME.setProperty(vertexBuilder, fileName, lumifyVisibility.getVisibility());
        FILE_NAME_EXTENSION.setProperty(vertexBuilder, FilenameUtils.getExtension(fileName), lumifyVisibility.getVisibility());
        MIME_TYPE.setProperty(vertexBuilder, mimeType, lumifyVisibility.getVisibility());
        RAW.setProperty(vertexBuilder, rawValue, lumifyVisibility.getVisibility());
        return vertexBuilder;
    }
}
