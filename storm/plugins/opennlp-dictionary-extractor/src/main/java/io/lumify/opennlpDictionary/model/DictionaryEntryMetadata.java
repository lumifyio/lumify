package io.lumify.opennlpDictionary.model;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

public class DictionaryEntryMetadata extends ColumnFamily{

    public static final String NAME = "Metadata";
    private static final String TOKENS_COLUMN = "tokens";
    private static final String CONCEPT_COLUMN = "concept";
    private static final String RESOLVED_NAME_COLUMN = "resolvedName";

    public DictionaryEntryMetadata() {
        super(NAME);
    }

    public String getTokens() {
        return Value.toString(get(TOKENS_COLUMN));
    }

    public DictionaryEntryMetadata setTokens (String tokens) {
        return (DictionaryEntryMetadata)set(TOKENS_COLUMN, tokens);
    }

    public String getConcept() {
        return Value.toString(get(CONCEPT_COLUMN));
    }

    public DictionaryEntryMetadata setConcept (String concept) {
        return (DictionaryEntryMetadata)set(CONCEPT_COLUMN,concept);
    }

    public String getResolvedName() {
        return Value.toString(get(RESOLVED_NAME_COLUMN));
    }

    public DictionaryEntryMetadata setResolvedName (String resolvedName) {
        return (DictionaryEntryMetadata)set(RESOLVED_NAME_COLUMN,resolvedName);
    }
}
