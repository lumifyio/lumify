package io.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;

public class BigTableArtifactThumbnail extends Row<BigTableArtifactThumbnailRowKey> {
    public static final String TABLE_NAME = "lumify_artifactThumbnail";

    public BigTableArtifactThumbnail(BigTableArtifactThumbnailRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public BigTableArtifactThumbnail(RowKey rowKey) {
        super(TABLE_NAME, new BigTableArtifactThumbnailRowKey(rowKey.toString()));
    }

    public BigTableArtifactThumbnailMetadata getMetadata() {
        BigTableArtifactThumbnailMetadata artifactThumbnailMetadata = get(BigTableArtifactThumbnailMetadata.NAME);
        if (artifactThumbnailMetadata == null) {
            addColumnFamily(new BigTableArtifactThumbnailMetadata());
        }
        return get(BigTableArtifactThumbnailMetadata.NAME);
    }
}
