package com.altamiracorp.lumify.core.model.resources;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;

public class Resource extends Row<ResourceRowKey> {
    public static final String TABLE_NAME = "atc_resource";

    public Resource(ResourceRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public Resource() {
        super(TABLE_NAME);
    }

    public Resource(RowKey rowKey) {
        this(new ResourceRowKey(rowKey.toString()));
    }

    public Resource(byte[] data, String contentType) {
        this(new ResourceRowKey(data));
        getContent().setData(data);
        getContent().setContentType(contentType);
    }

    public ResourceContent getContent() {
        ResourceContent resourceContent = get(ResourceContent.NAME);
        if (resourceContent == null) {
            addColumnFamily(new ResourceContent());
        }
        return get(ResourceContent.NAME);
    }

}
