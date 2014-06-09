package io.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import org.securegraph.Visibility;
import org.json.JSONException;
import org.json.JSONObject;

public class AuditEntity extends ColumnFamily {
    public static final String ENTITY_AUDIT = "entityAudit";
    public static final String ANALYZED_BY = "analyzedBy";
    public static final String NAME = "entity";

    public AuditEntity() {
        super(NAME);
    }

    public String getAnalyzedBy () {
        Value value = get(ANALYZED_BY);

        return value != null ? Value.toString(get(ANALYZED_BY)) : null;
    }

    public AuditEntity setAnalyzedBy (Object analyzedBy, Visibility visibility) {
        set (ANALYZED_BY, analyzedBy, visibility.getVisibilityString());
        return this;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("analyzedBy", this.getAnalyzedBy());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
