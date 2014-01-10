package com.altamiracorp.lumify.core.model.artifact;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.fs.FileSystemSession;
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.ingest.video.VideoPlaybackDetails;
import com.altamiracorp.lumify.core.model.SaveFileResults;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class ArtifactRepository extends Repository<Artifact> {
    public static final String VIDEO_STORAGE_HDFS_PATH = "/lumify/artifacts/video";
    public static final String LUMIFY_VIDEO_PREVIEW_HDFS_PATH = VIDEO_STORAGE_HDFS_PATH + "/preview/";
    public static final String LUMIFY_VIDEO_POSTER_FRAME_HDFS_PATH = VIDEO_STORAGE_HDFS_PATH + "/posterFrame/";
    public static final int FRAMES_PER_PREVIEW = 20;
    public static final int PREVIEW_FRAME_WIDTH = 360;
    public static final int PREVIEW_FRAME_HEIGHT = 240;
    private final ArtifactBuilder artifactBuilder = new ArtifactBuilder();
    private final FileSystemSession fsSession;
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public ArtifactRepository(
            final ModelSession modelSession,
            final FileSystemSession fsSession,
            final Graph graph,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository) {
        super(modelSession);
        this.fsSession = fsSession;
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public Row toRow(Artifact artifact) {
        return artifact;
    }

    @Override
    public String getTableName() {
        return artifactBuilder.getTableName();
    }

    @Override
    public Artifact fromRow(Row row) {
        return artifactBuilder.fromRow(row);
    }

    public SaveFileResults saveFile(InputStream in, User user) {
        return fsSession.saveFile(in);
    }

    public InputStream getRaw(Artifact artifact, Vertex vertex, User user) {
        byte[] bytes = artifact.getMetadata().getRaw();
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        }

        String hdfsPath = vertex.getPropertyValue(PropertyName.RAW_HDFS_PATH.toString(), 0).toString();
        if (hdfsPath != null) {
            return fsSession.loadFile(hdfsPath);
        }

        return null;
    }

    public Vertex saveToGraph(Artifact artifact, ArtifactExtractedInfo artifactExtractedInfo, User user) {
        Visibility visibility = new Visibility(""); // TODO set visibility

        Vertex artifactVertex = null;
        Object oldGraphVertexId = artifact.getMetadata().getGraphVertexId();
        boolean newVertex = false;
        if (oldGraphVertexId != null) {
            artifactVertex = graph.getVertex(oldGraphVertexId, user.getAuthorizations());
        }
        if (artifactVertex == null) {
            artifactVertex = graph.addVertex(visibility);
            newVertex = true;
        }

        List<Property> modifiedProperties = new ArrayList<Property>();
        modifiedProperties.add(graph.createProperty(PropertyName.ROW_KEY.toString(), artifact.getRowKey().toString(), visibility));
        modifiedProperties.add(graph.createProperty(PropertyName.CONCEPT_TYPE.toString(), ontologyRepository.getConceptByName(artifactExtractedInfo.getConceptType(), user).getId(), visibility));
        modifiedProperties.add(graph.createProperty(PropertyName.TITLE.toString(), artifactExtractedInfo.getTitle(), visibility));

        if (artifactExtractedInfo.getSource() != null) {
            modifiedProperties.add(graph.createProperty(PropertyName.SOURCE.toString(), artifactExtractedInfo.getSource(), visibility));
        }
        if (artifactExtractedInfo.getRawHdfsPath() != null) {
            modifiedProperties.add(graph.createProperty(PropertyName.RAW_HDFS_PATH.toString(), artifactExtractedInfo.getRawHdfsPath(), visibility));
        }
        if (artifactExtractedInfo.getTextHdfsPath() != null) {
            modifiedProperties.add(graph.createProperty(PropertyName.TEXT_HDFS_PATH.toString(), artifactExtractedInfo.getTextHdfsPath(), visibility));
            modifiedProperties.add(graph.createProperty(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH.toString(), artifactExtractedInfo.getTextHdfsPath(), visibility));
        }
        if (artifactExtractedInfo.getDetectedObjects() != null) {
            modifiedProperties.add(graph.createProperty(PropertyName.DETECTED_OBJECTS.toString(), artifactExtractedInfo.getDetectedObjects(), visibility));
        }
        if (artifactExtractedInfo.getDate() != null) {
            modifiedProperties.add(graph.createProperty(PropertyName.PUBLISHED_DATE.toString(), artifactExtractedInfo.getDate().getTime(), visibility));
        }
        if (artifactExtractedInfo.getAuthor() != null && !artifactExtractedInfo.getAuthor().equals("")) {
            modifiedProperties.add(graph.createProperty(PropertyName.AUTHOR.toString(), artifactExtractedInfo.getAuthor(), visibility));
        }
        Object artifactVertexId = artifactVertex.getId();

        if (newVertex) {
            auditRepository.auditVertexCreate(artifactVertexId, artifactExtractedInfo.getProcess(), "", user);
        }

        artifactVertex.setProperties(modifiedProperties.toArray(new Property[modifiedProperties.size()]));

        for (Property modifiedProperty : modifiedProperties) {
            auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), artifactVertex, modifiedProperty.getName(), artifactExtractedInfo.getProcess(), "", user);
        }

        if (!artifactVertexId.equals(oldGraphVertexId)) {
            artifact.getMetadata().setGraphVertexId(artifactVertexId);
            save(artifact, user.getModelUserContext());
        }
        return artifactVertex;
    }

    public InputStream getHighlightedText(Vertex artifactVertex, User user) throws IOException {
        checkNotNull(artifactVertex);
        checkNotNull(user);

        String hdfsPath = (String) artifactVertex.getPropertyValue(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH.toString(), 0);
        if (hdfsPath == null) {
            String artifactRowKey = (String) artifactVertex.getPropertyValue(PropertyName.ROW_KEY.toString(), 0);
            if (artifactRowKey != null) {
                Artifact artifact = findByRowKey(artifactRowKey, user.getModelUserContext());
                if (artifact != null) {
                    ArtifactMetadata metadata = artifact.getMetadata();
                    if (metadata != null) {
                        String highlightedText = metadata.getHighlightedText();
                        if (highlightedText != null) {
                            return new ByteArrayInputStream(highlightedText.getBytes());
                        }
                    }
                }
            }
            return null;
        } else {
            return fsSession.loadFile(hdfsPath);
        }
    }

    public InputStream getVideoPreviewImage(ArtifactRowKey artifactRowKey) {
        return fsSession.loadFile(getVideoPreviewPath(artifactRowKey.toString()));
    }

    public static String getVideoPreviewPath(String artifactRowKey) {
        return LUMIFY_VIDEO_PREVIEW_HDFS_PATH + artifactRowKey;
    }

    public InputStream getRawPosterFrame(String artifactRowKey) {
        return fsSession.loadFile(LUMIFY_VIDEO_POSTER_FRAME_HDFS_PATH + artifactRowKey);
    }

    public VideoPlaybackDetails getVideoPlaybackDetails(String artifactRowKey, String videoType) {
        checkNotNull(artifactRowKey);
        checkNotNull(videoType);

        String path = String.format("%s/%s/%s", VIDEO_STORAGE_HDFS_PATH, videoType, artifactRowKey);
        return new VideoPlaybackDetails(fsSession.loadFile(path), fsSession.getFileLength(path));
    }

    public ArtifactRowKey findRowKeyByGraphVertexId(String graphVertexId, User user) {
        Vertex vertex = graph.getVertex(graphVertexId, user.getAuthorizations());
        if (vertex == null) {
            return null;
        }
        return new ArtifactRowKey(vertex.getPropertyValue(PropertyName.ROW_KEY.toString(), 0).toString());
    }
}
