package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;

import java.io.InputStream;

public abstract class GraphPropertyWorker {
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) {

    }

    public abstract GraphPropertyWorkResult execute(InputStream in, GraphPropertyWorkData data) throws Exception;

    public abstract boolean isHandled(Vertex vertex, Property property);

    public boolean isLocalFileRequired() {
        return false;
    }
}
