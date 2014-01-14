package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;

public class TermMentionModel extends Row<TermMentionRowKey> {
    public static final String TABLE_NAME = "atc_termMention";

    public TermMentionModel(TermMentionRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public TermMentionModel() {
        super(TABLE_NAME);
    }

    public TermMentionModel(RowKey rowKey) {
        this(new TermMentionRowKey(rowKey.toString()));
    }

    public TermMentionMetadata getMetadata() {
        TermMentionMetadata termMentionMetadata = get(TermMentionMetadata.NAME);
        if (termMentionMetadata == null) {
            addColumnFamily(new TermMentionMetadata());
        }
        return get(TermMentionMetadata.NAME);
    }
}
