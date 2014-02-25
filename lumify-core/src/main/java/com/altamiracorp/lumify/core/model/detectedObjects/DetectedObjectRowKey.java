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

    public String getVertexId() {
        String[] keyElements = this.toString().split(":");
        int elementsToGet = keyElements.length - 2;
        String result = "";
        for (int i = 0; i < elementsToGet; i++) {
            if (i != 0) {
                result += ":";
            }
            result += keyElements[i];
        }
        return result;
    }

    public long getId() {
        String[] keyElements = this.toString().split(":");
        String startOffsetPadded = keyElements[keyElements.length - 1];
        return Long.parseLong(startOffsetPadded);
    }
}
