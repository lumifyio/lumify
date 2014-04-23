package io.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;

public class ArtifactThumbnail extends Row<ArtifactThumbnailRowKey> {
    public static final String TABLE_NAME = "atc_artifactThumbnail";

    public ArtifactThumbnail(ArtifactThumbnailRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public ArtifactThumbnail(RowKey rowKey) {
        super(TABLE_NAME, new ArtifactThumbnailRowKey(rowKey.toString()));
    }

    public ArtifactThumbnailMetadata getMetadata() {
        ArtifactThumbnailMetadata artifactThumbnailMetadata = get(ArtifactThumbnailMetadata.NAME);
        if (artifactThumbnailMetadata == null) {
            addColumnFamily(new ArtifactThumbnailMetadata());
        }
        return get(ArtifactThumbnailMetadata.NAME);
    }
}
