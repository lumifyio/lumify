package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

/**
 * This column family stores the user's rowkey as the column name and their permissions
 * as the column value for a particular workspace.
 */

public class WorkspacePermissions extends ColumnFamily {
    public static final String NAME = "users";
    public static final String USER = "user";

    public WorkspacePermissions() {
        super(NAME);
    }

    public String getUsers () {
        return Value.toString(get(USER));
    }
}
