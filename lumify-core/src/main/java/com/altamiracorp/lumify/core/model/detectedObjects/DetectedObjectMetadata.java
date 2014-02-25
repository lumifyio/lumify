package com.altamiracorp.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

public class DetectedObjectMetadata extends ColumnFamily {
    public static final String NAME = "Metadata";
    public static final String CLASSIFER_CONCEPT = "classiferConcept";
    public static final String X1 = "x1";
    public static final String X2 = "x2";
    public static final String Y1 = "y1";
    public static final String Y2 = "y2";

    public DetectedObjectMetadata() {
        super(NAME);
    }

    public DetectedObjectMetadata setClassifierConcept(String classifierConcept) {
        set(CLASSIFER_CONCEPT, classifierConcept);
        return this;
    }

    public String getClassiferConcept() {
        return Value.toString(get(CLASSIFER_CONCEPT));
    }

    public DetectedObjectMetadata setX1(long x1) {
        set(X1, x1);
        return this;
    }

    public long getX1() {
        return Value.toLong(get(X1));
    }

    public DetectedObjectMetadata setX2(long x2) {
        set(X2, x2);
        return this;
    }

    public long getX2() {
        return Value.toLong(get(X2));
    }

    public DetectedObjectMetadata setY1(long y1) {
        set(Y1, y1);
        return this;
    }

    public long getY1() {
        return Value.toLong(get(Y1));
    }

    public DetectedObjectMetadata setY2(long y2) {
        set(Y2, y2);
        return this;
    }

    public long getY2() {
        return Value.toLong(get(Y2));
    }
}
