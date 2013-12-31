package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import org.json.JSONException;
import org.json.JSONObject;

public class UserRow extends Row<UserRowKey> {
    public static final String TABLE_NAME = "atc_user";

    public UserRow(UserRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public UserRow(RowKey rowKey) {
        super(TABLE_NAME, new UserRowKey(rowKey.toString()));
    }

    public UserRow() {
        super(TABLE_NAME);
    }

    @Override
    public UserRowKey getRowKey() {
        UserRowKey rowKey = super.getRowKey();
        if (rowKey == null) {
            rowKey = new UserRowKey(getMetadata().getUserName());
        }
        return rowKey;
    }

    public UserMetadata getMetadata() {
        UserMetadata userMetadata = get(UserMetadata.NAME);
        if (userMetadata == null) {
            addColumnFamily(new UserMetadata());
        }
        return get(UserMetadata.NAME);
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("rowKey", getRowKey().toString());
            json.put("userName", getMetadata().getUserName());
            json.put("status", getMetadata().getStatus().toString().toLowerCase());
            json.put("userType", getMetadata().getUserType());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
