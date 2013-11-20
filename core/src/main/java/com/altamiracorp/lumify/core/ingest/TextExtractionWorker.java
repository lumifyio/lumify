package com.altamiracorp.lumify.core.ingest;

import com.altamiracorp.lumify.core.user.User;

import java.util.Map;

public interface TextExtractionWorker {
    void prepare(Map stormConf, User user);
}
