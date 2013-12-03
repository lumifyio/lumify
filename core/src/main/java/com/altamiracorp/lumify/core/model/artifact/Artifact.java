package com.altamiracorp.lumify.core.model.artifact;

import org.json.JSONException;
import org.json.JSONObject;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;

public class Artifact extends Row<ArtifactRowKey> {
    public static final long MAX_SIZE_OF_INLINE_FILE = 512 * 1024; // 512kiB
    public static final String TABLE_NAME = "atc_artifact";

    public Artifact(RowKey rowKey) {
        super(TABLE_NAME, new ArtifactRowKey(rowKey.toString()));
    }

    public Artifact(String rowKey) {
        super(TABLE_NAME, new ArtifactRowKey(rowKey));
    }

    public Artifact() {
        super(TABLE_NAME);
    }

    public ArtifactMetadata getMetadata() {
        ArtifactMetadata artifactMetadata = get(ArtifactMetadata.NAME);
        if (artifactMetadata == null) {
            addColumnFamily(new ArtifactMetadata());
        }
        return get(ArtifactMetadata.NAME);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        try {
            json.put("type", getType().toString().toLowerCase());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    public ArtifactType getType() {
        ArtifactMetadata metadata = getMetadata();
        String mimeType = metadata.getMimeType();

        if (mimeType == null) {
            return null;
        }

        String mimeTypeLowercase = mimeType.toLowerCase();
        if (mimeTypeLowercase.contains("video") || mimeTypeLowercase.contains("mp4")) {
            return ArtifactType.VIDEO;
        } else if (mimeTypeLowercase.contains("image")) {
            return ArtifactType.IMAGE;
        } else {
            return ArtifactType.DOCUMENT;
        }
    }
}
