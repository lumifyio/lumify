package io.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.RowKey;
import io.lumify.core.util.RowKeyHelper;
import org.apache.commons.lang.StringUtils;
import org.securegraph.SecureGraphException;

public class TermMentionRowKey extends RowKey {
    public static char ROW_KEY_SEP = '\u001e';

    public TermMentionRowKey(String rowKey) {
        super(rowKey);
    }

    public TermMentionRowKey(String graphVertexId, String propertyKey, long startOffset, long endOffset) {
        this(buildKey(graphVertexId, propertyKey, startOffset, endOffset));
    }

    public TermMentionRowKey(String graphVertexId, String propertyKey, long startOffset, long endOffset, String uniqueId) {
        this(buildKey(graphVertexId, propertyKey, startOffset, endOffset, uniqueId));
    }

    private static String buildKey(String graphVertexId, String propertyKey, long startOffset, long endOffset) {
        assertDoesNotContainRowKeySeparator(graphVertexId, "graphVertexId");
        assertDoesNotContainRowKeySeparator(propertyKey, "propertyKey");
        return graphVertexId
                + ROW_KEY_SEP
                + propertyKey
                + ROW_KEY_SEP
                + StringUtils.leftPad(Long.toString(endOffset), RowKeyHelper.OFFSET_WIDTH, '0')
                + ROW_KEY_SEP
                + StringUtils.leftPad(Long.toString(startOffset), RowKeyHelper.OFFSET_WIDTH, '0');
    }

    private static String buildKey(String graphVertexId, String propertyKey, long startOffset, long endOffset, String uniqueId) {
        assertDoesNotContainRowKeySeparator(graphVertexId, "graphVertexId");
        assertDoesNotContainRowKeySeparator(propertyKey, "propertyKey");
        assertDoesNotContainRowKeySeparator(uniqueId, "uniqueId");
        return graphVertexId
                + ROW_KEY_SEP
                + propertyKey
                + ROW_KEY_SEP
                + StringUtils.leftPad(Long.toString(endOffset), RowKeyHelper.OFFSET_WIDTH, '0')
                + ROW_KEY_SEP
                + StringUtils.leftPad(Long.toString(startOffset), RowKeyHelper.OFFSET_WIDTH, '0')
                + ROW_KEY_SEP
                + uniqueId;
    }

    private static void assertDoesNotContainRowKeySeparator(String value, String valueName) {
        if (value.indexOf(ROW_KEY_SEP) >= 0) {
            throw new SecureGraphException(valueName + " \"" + value + "\" cannot contain a row key separator");
        }
    }

    public String getGraphVertexId() {
        String[] keyElements = this.toString().split("" + ROW_KEY_SEP);
        return keyElements[0];
    }

    public String getPropertyKey() {
        String[] keyElements = this.toString().split("" + ROW_KEY_SEP);
        return keyElements[1];
    }

    public long getStartOffset() {
        String[] keyElements = this.toString().split("" + ROW_KEY_SEP);
        if (keyElements.length == 4) {
            return Long.parseLong(keyElements[keyElements.length - 1]);
        }
        return Long.parseLong(keyElements[keyElements.length - 2]);
    }

    public long getEndOffset() {
        String[] keyElements = this.toString().split("" + ROW_KEY_SEP);
        if (keyElements.length == 4) {
            return Long.parseLong(keyElements[keyElements.length - 2]);
        }
        return Long.parseLong(keyElements[keyElements.length - 3]);
    }

    public String getUniqueId() {
        String[] keyElements = this.toString().split("" + ROW_KEY_SEP);
        if (keyElements.length == 5) {
            return keyElements[keyElements.length - 1];
        }
        return null;
    }
}
