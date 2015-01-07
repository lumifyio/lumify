package io.lumify.core.ingest.graphProperty;

import io.lumify.core.user.User;
import org.securegraph.Authorizations;
import com.google.inject.Injector;
import org.apache.hadoop.fs.FileSystem;

import java.util.Map;

public class TermMentionFilterPrepareData {
    private final Map configuration;
    private final FileSystem hdfsFileSystem;
    private final User user;
    private final Authorizations authorizations;
    private final Injector injector;

    public TermMentionFilterPrepareData(Map configuration, FileSystem hdfsFileSystem, User user, Authorizations authorizations, Injector injector) {
        this.configuration = configuration;
        this.hdfsFileSystem = hdfsFileSystem;
        this.user = user;
        this.authorizations = authorizations;
        this.injector = injector;
    }

    public Map getConfiguration() {
        return configuration;
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
