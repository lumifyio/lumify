package io.lumify.opennlpDictionary.model;

import com.altamiracorp.bigtable.model.RowKey;
import io.lumify.core.util.RowKeyHelper;

public class DictionaryEntryRowKey extends RowKey {

    public DictionaryEntryRowKey(String rowKey) {
        super(rowKey);
    }

    public DictionaryEntryRowKey(String tokens, String concept) {
        this(RowKeyHelper.buildMinor(tokens,concept));
    }
}
