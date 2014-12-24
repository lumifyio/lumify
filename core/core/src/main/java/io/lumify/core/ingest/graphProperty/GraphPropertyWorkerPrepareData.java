package io.lumify.core.ingest.graphProperty;

import io.lumify.core.user.User;
import org.securegraph.Authorizations;
import com.google.inject.Injector;
import org.apache.hadoop.fs.FileSystem;

import java.util.List;
import java.util.Map;

public class GraphPropertyWorkerPrepareData {
    private final Map configuration;
    private final List<TermMentionFilter> termMentionFilters;
    private final FileSystem hdfsFileSystem;
    private final User user;
    private final Authorizations authorizations;
    private final Injector injector;

    public GraphPropertyWorkerPrepareData(Map configuration, List<TermMentionFilter> termMentionFilters, FileSystem hdfsFileSystem, User user, Authorizations authorizations, Injector injector) {
        this.configuration = configuration;
        this.termMentionFilters = termMentionFilters;
        this.hdfsFileSystem = hdfsFileSystem;
        this.user = user;
        this.authorizations = authorizations;
        this.injector = injector;
    }

    public Iterable<TermMentionFilter> getTermMentionFilters() {
        return termMentionFilters;
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
