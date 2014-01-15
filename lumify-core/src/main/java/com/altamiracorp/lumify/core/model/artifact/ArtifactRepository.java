package com.altamiracorp.lumify.core.model.artifact;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.fs.FileSystemSession;
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.ingest.video.VideoPlaybackDetails;
import com.altamiracorp.lumify.core.model.SaveFileResults;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.ElementMutation;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class ArtifactRepository extends Repository<Artifact> {
    public static final String VIDEO_STORAGE_HDFS_PATH = "/lumify/artifacts/video";
    public static final String LUMIFY_VIDEO_PREVIEW_HDFS_PATH = VIDEO_STORAGE_HDFS_PATH + "/preview/";
    public static final String LUMIFY_VIDEO_POSTER_FRAME_HDFS_PATH = VIDEO_STORAGE_HDFS_PATH + "/posterFrame/";
    private static final String LUMIFY_RAW_HDFS_PATH_FMT = "/lumify/artifacts/raw/%s";
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

        ElementMutation mutation = artifactVertex.prepareMutation();

        mutation.setProperty(PropertyName.ROW_KEY.toString(), artifact.getRowKey().toString(), visibility);
        mutation.setProperty(PropertyName.CONCEPT_TYPE.toString(), ontologyRepository.getConceptByName(artifactExtractedInfo.getConceptType(), user).getId(), visibility);
        mutation.setProperty(PropertyName.TITLE.toString(), artifactExtractedInfo.getTitle(), visibility);

        if (artifactExtractedInfo.getSource() != null) {
            mutation.setProperty(PropertyName.SOURCE.toString(), artifactExtractedInfo.getSource(), visibility);
        }
        if (artifactExtractedInfo.getRawHdfsPath() != null) {
            mutation.setProperty(PropertyName.RAW_HDFS_PATH.toString(), artifactExtractedInfo.getRawHdfsPath(), visibility);
        }
        if (artifactExtractedInfo.getTextHdfsPath() != null) {
            mutation.setProperty(PropertyName.TEXT_HDFS_PATH.toString(), artifactExtractedInfo.getTextHdfsPath(), visibility);
            mutation.setProperty(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH.toString(), artifactExtractedInfo.getTextHdfsPath(), visibility);
        }
        if (artifactExtractedInfo.getDetectedObjects() != null) {
            mutation.setProperty(PropertyName.DETECTED_OBJECTS.toString(), artifactExtractedInfo.getDetectedObjects(), visibility);
        }
        if (artifactExtractedInfo.getDate() != null) {
            mutation.setProperty(PropertyName.PUBLISHED_DATE.toString(), artifactExtractedInfo.getDate().getTime(), visibility);
        }
        if (artifactExtractedInfo.getAuthor() != null && !artifactExtractedInfo.getAuthor().equals("")) {
            mutation.setProperty(PropertyName.AUTHOR.toString(), artifactExtractedInfo.getAuthor(), visibility);
        }
        Object artifactVertexId = artifactVertex.getId();

        if (newVertex) {
            auditRepository.auditVertexCreate(artifactVertexId, artifactExtractedInfo.getProcess(), "", user);
        }

        mutation.save();

        // TODO replace this with a secure graph property listener that can audit
//        for (Property modifiedProperty : modifiedProperties) {
//            auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), artifactVertex, modifiedProperty.getName(), artifactExtractedInfo.getProcess(), "", user);
//        }

        if (!artifactVertexId.equals(oldGraphVertexId)) {
            artifact.getMetadata().setGraphVertexId(artifactVertexId);
            save(artifact, user.getModelUserContext());
        }
        return artifactVertex;
    }

    public Vertex saveArtifact(final ArtifactExtractedInfo artifactExtractedInfo, final User user) {
        Artifact artifact = saveArtifactModel(artifactExtractedInfo, user.getModelUserContext());
        String url = artifactExtractedInfo.getUrl();
        if (url != null && !url.trim().isEmpty()) {
            artifactExtractedInfo.setSource(url.trim());
        }
        return saveToGraph(artifact, artifactExtractedInfo, user);
    }

    private Artifact saveArtifactModel(final ArtifactExtractedInfo artifactExtractedInfo, final ModelUserContext userContext) {
        Artifact artifact = findByRowKey(artifactExtractedInfo.getRowKey(), userContext);
        if (artifact == null) {
            artifact = new Artifact(artifactExtractedInfo.getRowKey());
            if (artifactExtractedInfo.getDate() != null) {
                artifact.getMetadata().setCreateDate(artifactExtractedInfo.getDate());
            } else {
                artifact.getMetadata().setCreateDate(new Date());
            }
        }
        byte[] rawBytes = artifactExtractedInfo.getRaw();
        if (rawBytes != null) {
            if (rawBytes.length > Artifact.MAX_SIZE_OF_INLINE_FILE) {
                String rawPath = String.format(LUMIFY_RAW_HDFS_PATH_FMT, artifactExtractedInfo.getRowKey());
                fsSession.saveFile(rawPath, new ByteArrayInputStream(rawBytes));
                artifactExtractedInfo.setRaw(null);
                artifact.getMetadata().set(PropertyName.RAW_HDFS_PATH.toString(), rawPath);
            } else {
                artifact.getMetadata().setRaw(rawBytes);
            }
        }
        if (artifactExtractedInfo.getVideoTranscript() != null) {
            artifact.getMetadata().setVideoTranscript(artifactExtractedInfo.getVideoTranscript());
            artifact.getMetadata().setVideoDuration(Long.toString(artifactExtractedInfo.getVideoDuration()));

            // TODO should we combine text like this? If the text ends up on HDFS the text here is technically invalid
            if (artifactExtractedInfo.getText() == null) {
                artifactExtractedInfo.setText(artifactExtractedInfo.getVideoTranscript().toString());
            } else {
                artifactExtractedInfo.setText(artifactExtractedInfo.getText() + artifactExtractedInfo.getVideoTranscript().toString());
            }
        }
        if (artifactExtractedInfo.getText() != null) {
            artifact.getMetadata().setText(artifactExtractedInfo.getText());
            if (artifact.getMetadata().getHighlightedText() == null) {
                artifact.getMetadata().setHighlightedText(artifactExtractedInfo.getText());
            }
        }
        if (artifactExtractedInfo.getMappingJson() != null) {
            artifact.getMetadata().setMappingJson(artifactExtractedInfo.getMappingJson());
        }
        if (artifactExtractedInfo.getTitle() != null) {
            artifact.getMetadata().setFileName(artifactExtractedInfo.getTitle());
        }
        if (artifactExtractedInfo.getFileExtension() != null) {
            artifact.getMetadata().setFileExtension(artifactExtractedInfo.getFileExtension());
        }
        if (artifactExtractedInfo.getMimeType() != null) {
            artifact.getMetadata().setMimeType(artifactExtractedInfo.getMimeType());
        }

        save(artifact, userContext);
        return artifact;
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
