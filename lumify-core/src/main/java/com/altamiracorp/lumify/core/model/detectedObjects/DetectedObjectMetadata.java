package com.altamiracorp.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.securegraph.Visibility;

public class DetectedObjectMetadata extends ColumnFamily {
    public static final String NAME = "Metadata";
    public static final String CLASSIFER_CONCEPT = "classiferConcept";
    public static final String X1 = "x1";
    public static final String X2 = "x2";
    public static final String Y1 = "y1";
    public static final String Y2 = "y2";
    public static final String RESOLVED_ID = "resolvedId";

    public DetectedObjectMetadata() {
        super(NAME);
    }

    public DetectedObjectMetadata setClassifierConcept(String classifierConcept, Visibility visibility) {
        set(CLASSIFER_CONCEPT, classifierConcept, visibility.getVisibilityString());
        return this;
    }

    public String getClassiferConcept() {
        return Value.toString(get(CLASSIFER_CONCEPT));
    }

    public DetectedObjectMetadata setX1(long x1, Visibility visibility) {
        set(X1, x1, visibility.getVisibilityString());
        return this;
    }

    public long getX1() {
        return Value.toLong(get(X1));
    }

    public DetectedObjectMetadata setX2(long x2, Visibility visibility) {
        set(X2, x2, visibility.getVisibilityString());
        return this;
    }

    public long getX2() {
        return Value.toLong(get(X2));
    }

    public DetectedObjectMetadata setY1(long y1, Visibility visibility) {
        set(Y1, y1, visibility.getVisibilityString());
        return this;
    }

    public long getY1() {
        return Value.toLong(get(Y1));
    }

    public DetectedObjectMetadata setY2(long y2, Visibility visibility) {
        set(Y2, y2, visibility.getVisibilityString());
        return this;
    }

    public long getY2() {
        return Value.toLong(get(Y2));
    }

    public DetectedObjectMetadata setResolvedId(Object id, Visibility visibility) {
        set(RESOLVED_ID, id, visibility.getVisibilityString());
        return this;
    }

    public Object getResolvedId() {
        return Value.toString(get(RESOLVED_ID));
    }
}
