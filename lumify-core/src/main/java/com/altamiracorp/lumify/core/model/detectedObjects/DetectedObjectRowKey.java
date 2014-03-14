package com.altamiracorp.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import org.apache.commons.lang.StringUtils;

public class DetectedObjectRowKey extends RowKey {
    public DetectedObjectRowKey(String rowKey) {
        super(rowKey);
    }

    public DetectedObjectRowKey(Object vertexId, Object id) {
        this(buildKey(vertexId, id));
    }

    private static String buildKey(Object vertexId, Object id) {
        return vertexId
                + ":"
                + StringUtils.leftPad(id.toString(), RowKeyHelper.OFFSET_WIDTH, '0');
    }

    public String getArtifactId() {
        String[] keyElements = this.toString().split(":");
        return keyElements[keyElements.length - 2];
    }

    public String getId() {
        String[] keyElements = this.toString().split(":");
        return keyElements[keyElements.length - 1];
    }
}
