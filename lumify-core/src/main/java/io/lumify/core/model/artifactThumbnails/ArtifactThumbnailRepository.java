package io.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.*;
import io.lumify.core.user.User;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public abstract class ArtifactThumbnailRepository extends Repository<ArtifactThumbnail> {
    public static int FRAMES_PER_PREVIEW = 20;
    public static int PREVIEW_FRAME_WIDTH = 360;
    public static int PREVIEW_FRAME_HEIGHT = 240;

    public ArtifactThumbnailRepository(ModelSession modelSession) {
        super(modelSession);
    }

    public abstract ArtifactThumbnail fromRow(Row row);

    public abstract Row toRow(ArtifactThumbnail artifactThumbnail);

    public abstract String getTableName();

    public abstract ArtifactThumbnail getThumbnail(Object artifactVertexId, String thumbnailType, int width, int height, User user);

    public abstract byte[] getThumbnailData(Object artifactVertexId, String thumbnailType, int width, int height, User user);

    public abstract ArtifactThumbnail createThumbnail(Object artifactVertexId, String thumbnailType, InputStream in, int[] boundaryDims, User user) throws IOException;

    public abstract int thumnbailType(BufferedImage image);

    public abstract String thumbnailFormat(BufferedImage image);

    public abstract int[] getScaledDimension(int[] imgSize, int[] boundary);
}