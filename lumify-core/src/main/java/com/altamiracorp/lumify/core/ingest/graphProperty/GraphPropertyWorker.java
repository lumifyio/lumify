package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;

import java.io.InputStream;

public abstract class GraphPropertyWorker {
    private Graph graph;
    private WorkQueueRepository workQueueRepository;

    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) {

    }

    public abstract GraphPropertyWorkResult execute(InputStream in, GraphPropertyWorkData data) throws Exception;

    public abstract boolean isHandled(Vertex vertex, Property property);

    public boolean isLocalFileRequired() {
        return false;
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    protected Graph getGraph() {
        return graph;
    }

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }
}
