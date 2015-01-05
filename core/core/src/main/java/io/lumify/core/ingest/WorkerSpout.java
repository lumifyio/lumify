package io.lumify.core.ingest;

public abstract class WorkerSpout {
    public void open() {
    }

    public void close() {

    }

    public void ack(Object msgId) {

    }

    public void fail(Object msgId) {

    }

    public abstract WorkerTuple nextTuple() throws Exception;
}
