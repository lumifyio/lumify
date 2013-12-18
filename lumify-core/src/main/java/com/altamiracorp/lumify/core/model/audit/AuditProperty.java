package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import org.json.JSONException;
import org.json.JSONObject;

public class AuditProperty extends ColumnFamily {
    public static final String NAME = "property";
    public static final String PREVIOUS_VALUE = "previousValue";
    public static final String NEW_VALUE = "newValue";

    public AuditProperty () {

        super (NAME);
    }

    public String getPreviousValue() {
        return Value.toString(get(PREVIOUS_VALUE));
    }

    public AuditProperty setPreviousValue (Object previousValue) {
        set (PREVIOUS_VALUE, previousValue);
        return this;
    }

    public String getNewValue() {
        return Value.toString(get(NEW_VALUE));
    }

    public AuditProperty setNewValue (Object newValue) {
        set (NEW_VALUE, newValue);
        return this;
    }

    @Override
    public JSONObject toJson () {
        try {
            JSONObject json = new JSONObject();
            json.put("previousValue", this.getPreviousValue());
            json.put("newValue", this.getNewValue());
            return  json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
