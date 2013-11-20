package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

public class WorkspaceContent extends ColumnFamily {
    public static final String NAME = "content";
    public static final String DATA = "data";

    public WorkspaceContent() {
        super(NAME);
    }

    public String getData() {
        return Value.toString(get(DATA));
    }

    public WorkspaceContent setData(String data) {
        set(DATA, data);
        return this;
    }
}
