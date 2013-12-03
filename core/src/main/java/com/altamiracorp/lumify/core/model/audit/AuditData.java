package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.lumify.core.user.User;

public class AuditData extends ColumnFamily {
    public static final String NAME = "data";
    public static final String MESSAGE = "message";
    public static final String USER_ROW_KEY = "userRowKey";

    public AuditData() {
        super(NAME);
    }

    public String getMessage() {
        return Value.toString(get(MESSAGE));
    }

    public AuditData setMessage(String message) {
        set(MESSAGE, message);
        return this;
    }

    public String getUserRowKey() {
        return Value.toString(get(USER_ROW_KEY));
    }

    public AuditData setUserRowKey(String userRowKey) {
        set(USER_ROW_KEY, userRowKey);
        return this;
    }

    public AuditData setUser(User user) {
        setUserRowKey(user.getRowKey());
        return this;
    }
}
