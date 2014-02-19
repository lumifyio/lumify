package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Visibility;

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

    public AuditData setMessage(String message, Visibility visibility) {
        set(MESSAGE, message, visibility.getVisibilityString());
        return this;
    }

    public Object getUserId() {
        return Value.toString(get(USER_ID));
    }

    public AuditData setUserId(Object userId, Visibility visibility) {
        set(USER_ID, userId.toString(), visibility.getVisibilityString());
        return this;
    }

    public AuditData setUser(User user, Visibility visibility) {
        setUserId(user.getUserId(), visibility);
        return this;
    }
}
