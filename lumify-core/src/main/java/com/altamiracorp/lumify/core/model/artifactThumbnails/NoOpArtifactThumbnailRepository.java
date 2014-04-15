package com.altamiracorp.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.user.User;
import com.beust.jcommander.internal.Nullable;
import com.google.inject.Inject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class NoOpArtifactThumbnailRepository extends ArtifactThumbnailRepository {
    @Inject
    public NoOpArtifactThumbnailRepository(@Nullable ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public ArtifactThumbnail fromRow(Row row) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Row toRow(ArtifactThumbnail artifactThumbnail) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String getTableName() {
        throw new RuntimeException("not supported");
    }

    @Override
    public ArtifactThumbnail getThumbnail(Object artifactVertexId, String thumbnailType, int width, int height, User user) {
        throw new RuntimeException("not supported");
    }

    @Override
    public byte[] getThumbnailData(Object artifactVertexId, String thumbnailType, int width, int height, User user) {
        throw new RuntimeException("not supported");
    }

    @Override
    public ArtifactThumbnail createThumbnail(Object artifactVertexId, String thumbnailType, InputStream in, int[] boundaryDims, User user) throws IOException {
        throw new RuntimeException("not supported");
    }

    @Override
    public int thumnbailType(BufferedImage image) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String thumbnailFormat(BufferedImage image) {
        throw new RuntimeException("not supported");
    }

    @Override
    public int[] getScaledDimension(int[] imgSize, int[] boundary) {
        throw new RuntimeException("not supported");
    }
}
