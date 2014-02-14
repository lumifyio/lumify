package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.lumify.core.user.User;

public class AuditData extends ColumnFamily {
    public static final String NAME = "data";
    public static final String MESSAGE = "message";
    public static final String USER_ID = "userId";

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

    public Object getUserId() {
        return Value.toString(get(USER_ID));
    }

    public AuditData setUserId(Object userId) {
        set(USER_ID, userId.toString());
        return this;
    }

    public AuditData setUser(User user) {
        setUserId(user.getUserId());
        return this;
    }
}
