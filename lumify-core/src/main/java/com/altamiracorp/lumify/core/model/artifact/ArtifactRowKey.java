package com.altamiracorp.lumify.core.model.artifact;

import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.util.RowKeyHelper;

public class ArtifactRowKey extends RowKey {
    public ArtifactRowKey(String rowKey) {
        super(rowKey);
    }

    public static ArtifactRowKey build(byte[] docArtifactBytes) {
        return new ArtifactRowKey(RowKeyHelper.buildSHA256KeyString(docArtifactBytes));
    }
}
