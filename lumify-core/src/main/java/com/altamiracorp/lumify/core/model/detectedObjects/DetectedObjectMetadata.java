package com.altamiracorp.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.securegraph.Visibility;

public class DetectedObjectMetadata extends ColumnFamily {
    public static final String NAME = "Metadata";
    public static final String CONCEPT = "classiferConcept";
    public static final String X1 = "x1";
    public static final String X2 = "x2";
    public static final String Y1 = "y1";
    public static final String Y2 = "y2";
    public static final String RESOLVED_ID = "resolvedId";
    public static final String PROCESS = "process";

    public DetectedObjectMetadata() {
        super(NAME);
    }

    public DetectedObjectMetadata setClassifierConcept(String classifierConcept, Visibility visibility) {
        set(CONCEPT, classifierConcept, visibility.getVisibilityString());
        return this;
    }

    public String getClassiferConcept() {
        return Value.toString(get(CONCEPT));
    }

    public DetectedObjectMetadata setX1(double x1, Visibility visibility) {
        set(X1, x1, visibility.getVisibilityString());
        return this;
    }

    public double getX1() {
        return Value.toDouble(get(X1));
    }

    public DetectedObjectMetadata setX2(double x2, Visibility visibility) {
        set(X2, x2, visibility.getVisibilityString());
        return this;
    }

    public double getX2() {
        return Value.toDouble(get(X2));
    }

    public DetectedObjectMetadata setY1(double y1, Visibility visibility) {
        set(Y1, y1, visibility.getVisibilityString());
        return this;
    }

    public double getY1() {
        return Value.toDouble(get(Y1));
    }

    public DetectedObjectMetadata setY2(double y2, Visibility visibility) {
        set(Y2, y2, visibility.getVisibilityString());
        return this;
    }

    public double getY2() {
        return Value.toDouble(get(Y2));
    }

    public DetectedObjectMetadata setResolvedId(Object id, Visibility visibility) {
        set(RESOLVED_ID, id, visibility.getVisibilityString());
        return this;
    }

    public String getResolvedId() {
        return Value.toString(get(RESOLVED_ID));
    }

    public String getProcess () {
        return Value.toString(get(PROCESS));
    }

    public DetectedObjectMetadata setProcess (Object process, Visibility visibility) {
        set (PROCESS, process, visibility.getVisibilityString());
        return this;
    }
}
