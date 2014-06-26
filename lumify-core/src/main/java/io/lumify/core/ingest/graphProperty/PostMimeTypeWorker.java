package io.lumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import org.apache.commons.io.IOUtils;
import org.securegraph.Authorizations;
import org.securegraph.Element;
import org.securegraph.Graph;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class PostMimeTypeWorker {
    private Graph graph;
    private WorkQueueRepository workQueueRepository;

    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {

    }

    public abstract void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception;

    protected File getLocalFileForRaw(Element element) throws IOException {
        StreamingPropertyValue rawValue = RawLumifyProperties.RAW.getPropertyValue(element);
        InputStream in = rawValue.getInputStream();
        try {
            File f = File.createTempFile("imageOrientation", "image");
            FileOutputStream out = new FileOutputStream(f);
            try {
                IOUtils.copy(in, out);
                return f;
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
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
