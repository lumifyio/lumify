package io.lumify.core.ingest.graphProperty;

public abstract class GraphPropertyWorkerSpout {
    public void open() {
    }

    public void close() {

    }

    public void ack(Object msgId) {

    }

    public void fail(Object msgId) {

    }

    public abstract GraphPropertyWorkerTuple nextTuple() throws Exception;
}
