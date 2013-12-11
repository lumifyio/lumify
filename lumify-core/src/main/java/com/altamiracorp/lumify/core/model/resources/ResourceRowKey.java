package com.altamiracorp.lumify.core.model.resources;

import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.util.RowKeyHelper;

public class ResourceRowKey extends RowKey {
    public ResourceRowKey(String rowKey) {
        super(rowKey);
    }

    public ResourceRowKey(byte[] data) {
        this(RowKeyHelper.buildSHA256KeyStringNoUrn(data));
    }
}
