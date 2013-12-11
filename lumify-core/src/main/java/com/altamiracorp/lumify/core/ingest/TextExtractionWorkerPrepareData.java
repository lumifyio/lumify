package com.altamiracorp.lumify.core.ingest;

import com.altamiracorp.lumify.core.user.User;
import com.google.inject.Injector;
import org.apache.hadoop.fs.FileSystem;

import java.util.Map;

public class TextExtractionWorkerPrepareData {
    private final FileSystem hdfsFileSystem;
    private final Map stormConf;
    private final User user;
    private final Injector injector;

    public TextExtractionWorkerPrepareData(Map stormConf, User user, FileSystem hdfsFileSystem, Injector injector) {
        this.stormConf = stormConf;
        this.user = user;
        this.hdfsFileSystem = hdfsFileSystem;
        this.injector = injector;
    }

    public Map getStormConf() {
        return stormConf;
    }

    public User getUser() {
        return user;
    }

    public FileSystem getHdfsFileSystem() {
        return hdfsFileSystem;
    }

    public Injector getInjector() {
        return injector;
    }
}
