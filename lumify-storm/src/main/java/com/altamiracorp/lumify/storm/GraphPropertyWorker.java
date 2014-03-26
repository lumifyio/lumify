package com.altamiracorp.lumify.storm;

import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;

import java.io.InputStream;

public abstract class GraphPropertyWorker {
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) {

    }

    public abstract GraphPropertyWorkResult execute(InputStream in, GraphPropertyWorkData data);

    public abstract boolean isHandled(Vertex vertex, Property property);
}
