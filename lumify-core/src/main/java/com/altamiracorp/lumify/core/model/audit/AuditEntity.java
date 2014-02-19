package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.securegraph.Visibility;
import org.json.JSONException;
import org.json.JSONObject;

public class AuditEntity extends ColumnFamily {
    public static final String NAME = "entity";
    public static final String TITLE = "title";
    public static final String TYPE = "type";
    public static final String SUBTYPE = "subtype";
    public static final String ID = "id";

    public AuditEntity() {
        super(NAME);
    }

    public String getTitle() {
        return Value.toString(get(TITLE));
    }

    public AuditEntity setTitle(Object title, Visibility visibility) {
        set(TITLE, title, visibility.getVisibilityString());
        return this;
    }

    public String getType() {
        return Value.toString(get(TYPE));
    }

    public AuditEntity setType(Object type, Visibility visibility) {
        set(TYPE, type, visibility.getVisibilityString());
        return this;
    }

    public String getSubtype() {
        return Value.toString(get(SUBTYPE));
    }

    public AuditEntity setSubtype(Object subtype, Visibility visibility) {
        set(SUBTYPE, subtype, visibility.getVisibilityString());
        return this;
    }

    public String getId() {
        return Value.toString(get(ID));
    }

    public AuditEntity setID(String id, Visibility visibility) {
        set(ID, id, visibility.getVisibilityString());
        return this;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("title", this.getTitle());
            json.put("type", this.getType());
            json.put("subType", this.getSubtype());
            json.put("id", this.getId());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
