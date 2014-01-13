package com.altamiracorp.lumify.web.session.model;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

public class JettySessionMetadata extends ColumnFamily {
    public static final String COLUMN_FAMILY_NAME = "metadata";
    public static final String CREATED = "created";
    public static final String ACCESSED = "accessed";
    public static final String CLUSTER_ID = "cluster_id";
    public static final String VERSION = "version";

    public JettySessionMetadata() {
        super(COLUMN_FAMILY_NAME);
    }

    public long getCreated() {
        return Value.toLong(get(CREATED));
    }

    public JettySessionMetadata setCreated(long created) {
        set(CREATED, created);
        return this;
    }

    public long getAccessed() {
        return Value.toLong(get(ACCESSED));
    }

    public JettySessionMetadata setAccessed(long accessed) {
        set(ACCESSED, accessed);
        return this;
    }

    public String getClusterId() {
        return Value.toString(get(CLUSTER_ID));
    }

    public JettySessionMetadata setClusterId(String clusterId) {
        set(CLUSTER_ID, clusterId);
        return this;
    }

    public long getVersion() {
        return Value.toLong(get(VERSION));
    }

    public JettySessionMetadata setVersion(long version) {
        set(VERSION, version);
        return this;
    }
}
