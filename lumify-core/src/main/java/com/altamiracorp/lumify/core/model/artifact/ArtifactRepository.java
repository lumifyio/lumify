package com.altamiracorp.lumify.core.model.artifact;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.fs.FileSystemSession;
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.ingest.video.VideoPlaybackDetails;
import com.altamiracorp.lumify.core.model.GraphSession;
import com.altamiracorp.lumify.core.model.SaveFileResults;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.graph.InMemoryGraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
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
    private final GraphSession graphSession;
    private final AuditRepository auditRepository;

    @Inject
    public ArtifactRepository(
            final ModelSession modelSession,
            final FileSystemSession fsSession,
            final GraphSession graphSession,
            final AuditRepository auditRepository) {
        super(modelSession);
        this.fsSession = fsSession;
        this.graphSession = graphSession;
        this.auditRepository = auditRepository;
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

    public InputStream getRaw(Artifact artifact, GraphVertex vertex, User user) {
        byte[] bytes = artifact.getMetadata().getRaw();
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        }

        String hdfsPath = vertex.getProperty(PropertyName.RAW_HDFS_PATH).toString();
        if (hdfsPath != null) {
            return fsSession.loadFile(hdfsPath);
        }

        return null;
    }

    public GraphVertex saveToGraph(Artifact artifact, ArtifactExtractedInfo artifactExtractedInfo, User user) {
        GraphVertex artifactVertex = null;
        String oldGraphVertexId = artifact.getMetadata().getGraphVertexId();
        boolean newVertex = false;
        if (oldGraphVertexId != null) {
            artifactVertex = graphSession.findGraphVertex(oldGraphVertexId, user);
        }
        if (artifactVertex == null) {
            artifactVertex = new InMemoryGraphVertex();
            newVertex = true;
        }

        artifactVertex.setProperty(PropertyName.ROW_KEY.toString(), artifact.getRowKey().toString());
        artifactVertex.setProperty(PropertyName.CONCEPT_TYPE, artifactExtractedInfo.getArtifactType());
        artifactVertex.setProperty(PropertyName.TITLE, artifactExtractedInfo.getTitle());

        List<String> modifiedProperties = Lists.newArrayList(PropertyName.ROW_KEY.toString(), PropertyName.CONCEPT_TYPE.toString(), PropertyName.TITLE.toString());

        if (artifactExtractedInfo.getSource() != null) {
            artifactVertex.setProperty(PropertyName.SOURCE, artifactExtractedInfo.getSource());
            modifiedProperties.add(PropertyName.SOURCE.toString());
        }
        if (artifactExtractedInfo.getRawHdfsPath() != null) {
            artifactVertex.setProperty(PropertyName.RAW_HDFS_PATH, artifactExtractedInfo.getRawHdfsPath());
            modifiedProperties.add(PropertyName.RAW_HDFS_PATH.toString());
        }
        if (artifactExtractedInfo.getTextHdfsPath() != null) {
            artifactVertex.setProperty(PropertyName.TEXT_HDFS_PATH, artifactExtractedInfo.getTextHdfsPath());
            artifactVertex.setProperty(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH, artifactExtractedInfo.getTextHdfsPath());
            Collections.addAll(modifiedProperties, PropertyName.TEXT_HDFS_PATH.toString(), PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH.toString());
        }
        if (artifactExtractedInfo.getDetectedObjects() != null) {
            artifactVertex.setProperty(PropertyName.DETECTED_OBJECTS, artifactExtractedInfo.getDetectedObjects());
            modifiedProperties.add(PropertyName.DETECTED_OBJECTS.toString());
        }
        if (artifactExtractedInfo.getDate() != null) {
            artifactVertex.setProperty(PropertyName.PUBLISHED_DATE, artifactExtractedInfo.getDate().getTime());
            modifiedProperties.add(PropertyName.PUBLISHED_DATE.toString());
        }
        if (artifactExtractedInfo.getAuthor() != null && !artifactExtractedInfo.getAuthor().equals("")) {
            artifactVertex.setProperty(PropertyName.AUTHOR, artifactExtractedInfo.getAuthor());
            modifiedProperties.add(PropertyName.AUTHOR.toString());
        }
        String artifactVertexId = graphSession.save(artifactVertex, user);

        if (newVertex) {
            auditRepository.auditVertexCreate(artifactVertexId, artifactExtractedInfo.getProcess(), "", user);
        }

        for (String modifiedProperty : modifiedProperties) {
            auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), artifactVertex, modifiedProperty, artifactExtractedInfo.getProcess(), "", user);
        }

        if (!artifactVertexId.equals(oldGraphVertexId)) {
            artifact.getMetadata().setGraphVertexId(artifactVertexId);
            save(artifact, user.getModelUserContext());
        }
        return artifactVertex;
    }

    public InputStream getHighlightedText(GraphVertex artifactVertex, User user) throws IOException {
        checkNotNull(artifactVertex);
        checkNotNull(user);

        String hdfsPath = (String) artifactVertex.getProperty(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH);
        if (hdfsPath == null) {
            String artifactRowKey = (String) artifactVertex.getProperty(PropertyName.ROW_KEY);
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
        GraphVertex vertex = graphSession.findGraphVertex(graphVertexId, user);
        if (vertex == null) {
            return null;
        }
        return new ArtifactRowKey(vertex.getProperty(PropertyName.ROW_KEY).toString());
    }
}
