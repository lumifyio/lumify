package io.lumify.core.ingest.graphProperty;

public abstract class PostMimeTypeWorker {
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {

    }

    public abstract void execute(String mimeType, GraphPropertyWorkData data) throws Exception;
}
