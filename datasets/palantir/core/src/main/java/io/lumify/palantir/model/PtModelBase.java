package io.lumify.palantir.model;

import org.apache.hadoop.io.Writable;

public abstract class PtModelBase implements Writable {
    public abstract Writable getKey();
}
