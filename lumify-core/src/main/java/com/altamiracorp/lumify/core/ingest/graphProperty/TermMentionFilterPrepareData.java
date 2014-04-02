package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Authorizations;
import com.google.inject.Injector;
import org.apache.hadoop.fs.FileSystem;

import java.util.Map;

public class TermMentionFilterPrepareData {
    private final Map stormConf;
    private final FileSystem hdfsFileSystem;
    private final User user;
    private final Authorizations authorizations;
    private final Injector injector;

    public TermMentionFilterPrepareData(Map stormConf, FileSystem hdfsFileSystem, User user, Authorizations authorizations, Injector injector) {
        this.stormConf = stormConf;
        this.hdfsFileSystem = hdfsFileSystem;
        this.user = user;
        this.authorizations = authorizations;
        this.injector = injector;
    }

    public Map getStormConf() {
        return stormConf;
    }

    public User getUser() {
        return user;
    }

    public Authorizations getAuthorizations() {
        return authorizations;
    }

    public Injector getInjector() {
        return injector;
    }

    public FileSystem getHdfsFileSystem() {
        return hdfsFileSystem;
    }
}
