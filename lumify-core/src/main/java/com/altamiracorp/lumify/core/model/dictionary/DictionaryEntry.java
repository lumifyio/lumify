package com.altamiracorp.lumify.core.model.dictionary;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import org.json.JSONObject;

public class DictionaryEntry extends Row<DictionaryEntryRowKey> {

    public static final String TABLE_NAME = "atc_dictionaryEntry";

    public DictionaryEntry(DictionaryEntryRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public DictionaryEntry(RowKey rowKey) {
        super(TABLE_NAME, new DictionaryEntryRowKey(rowKey.toString()));
    }

    public DictionaryEntryMetadata getMetadata() {
        DictionaryEntryMetadata metadata = get(DictionaryEntryMetadata.NAME);
        if (metadata == null) {
            addColumnFamily(new DictionaryEntryMetadata());
        }
        return get(DictionaryEntryMetadata.NAME);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        json.put(LumifyProperties.ROW_KEY.getKey(), getRowKey().toString());
        json.put("concept", getMetadata().getConcept());
        json.put("tokens", getMetadata().getTokens());
        if (getMetadata().getResolvedName() != null) {
            json.put("resolvedName", getMetadata().getResolvedName());
        }

        return json;
    }

}
