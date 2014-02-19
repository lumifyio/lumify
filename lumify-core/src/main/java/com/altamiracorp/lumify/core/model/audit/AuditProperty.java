package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.securegraph.Visibility;
import org.json.JSONException;
import org.json.JSONObject;

public class AuditProperty extends ColumnFamily {
    public static final String NAME = "property";
    public static final String PREVIOUS_VALUE = "previousValue";
    public static final String NEW_VALUE = "newValue";
    public static final String PROPERTY_NAME = "propertyName";
    public static final String PROPERTY_METADATA = "propertyMetadata";

    public AuditProperty() {
        super(NAME);
    }

    public String getPreviousValue() {
        return Value.toString(get(PREVIOUS_VALUE));
    }

    public AuditProperty setPreviousValue(Object previousValue, Visibility visibility) {
        set(PREVIOUS_VALUE, previousValue, visibility.getVisibilityString());
        return this;
    }

    public String getNewValue() {
        return Value.toString(get(NEW_VALUE));
    }

    public AuditProperty setNewValue(Object newValue, Visibility visibility) {
        set(NEW_VALUE, newValue, visibility.getVisibilityString());
        return this;
    }

    public String getPropertyName() {
        return Value.toString(get(PROPERTY_NAME));
    }

    public AuditProperty setPropertyName(Object propertyName, Visibility visibility) {
        set(PROPERTY_NAME, propertyName, visibility.getVisibilityString());
        return this;
    }

    public JSONObject getPropertyMetadata() {
        return Value.toJson(get(PROPERTY_METADATA));
    }

    public AuditProperty setPropertyMetadata(Object propertyMetadata, Visibility visibility) {
        set(PROPERTY_METADATA, propertyMetadata, visibility.getVisibilityString());
        return this;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put(PREVIOUS_VALUE, this.getPreviousValue());
            json.put(NEW_VALUE, this.getNewValue());
            json.put(PROPERTY_NAME, this.getPropertyName());
            if (this.getPropertyMetadata() != null) {
                json.put(PROPERTY_METADATA, this.getPropertyMetadata());
            }
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
