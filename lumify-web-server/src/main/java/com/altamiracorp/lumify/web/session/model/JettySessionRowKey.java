package com.altamiracorp.lumify.web.session.model;

import com.altamiracorp.bigtable.model.RowKey;

public class JettySessionRowKey extends RowKey {
    public JettySessionRowKey(String rowKey) {
        super(rowKey);
    }
}
