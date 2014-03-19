package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import org.apache.commons.lang.StringUtils;

public class TermMentionRowKey extends RowKey {
    public TermMentionRowKey(String rowKey) {
        super(rowKey);
    }

    public TermMentionRowKey(String graphVertexId, long startOffset, long endOffset) {
        this(buildKey(graphVertexId, startOffset, endOffset));
    }

    public TermMentionRowKey(String graphVertexId, long startOffset, long endOffset, String uniqueId) {
        this(buildKey(graphVertexId, startOffset, endOffset, uniqueId));
    }

    private static String buildKey(String graphVertexId, long startOffset, long endOffset) {
        return graphVertexId
                + ":"
                + StringUtils.leftPad(Long.toString(endOffset), RowKeyHelper.OFFSET_WIDTH, '0')
                + ":"
                + StringUtils.leftPad(Long.toString(startOffset), RowKeyHelper.OFFSET_WIDTH, '0');
    }

    private static String buildKey(String graphVertexId, long startOffset, long endOffset, String uniqueId) {
        return graphVertexId
                + ":"
                + StringUtils.leftPad(Long.toString(endOffset), RowKeyHelper.OFFSET_WIDTH, '0')
                + ":"
                + StringUtils.leftPad(Long.toString(startOffset), RowKeyHelper.OFFSET_WIDTH, '0')
                + ":"
                + uniqueId;
    }

    public String getGraphVertexId() {
        String[] keyElements = this.toString().split(":");
        int elementsToGet = keyElements.length - 3;
        String result = "";
        for (int i = 0; i < elementsToGet; i++) {
            if (i != 0) {
                result += ":";
            }
            result += keyElements[i];
        }
        return result;
    }

    public long getStartOffset() {
        String[] keyElements = this.toString().split(":");
        if (keyElements.length != 4) {
            return Long.parseLong(keyElements[keyElements.length - 1]);
        }
        return Long.parseLong(keyElements[keyElements.length - 2]);
    }

    public long getEndOffset() {
        String[] keyElements = this.toString().split(":");
        if (keyElements.length != 4) {
            return Long.parseLong(keyElements[keyElements.length - 2]);
        }
        return Long.parseLong(keyElements[keyElements.length - 3]);
    }
}
