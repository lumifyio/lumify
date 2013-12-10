package com.altamiracorp.lumify.core.ingest;

public interface TextExtractionWorker {
    void prepare(TextExtractionWorkerPrepareData data) throws Exception;
}
