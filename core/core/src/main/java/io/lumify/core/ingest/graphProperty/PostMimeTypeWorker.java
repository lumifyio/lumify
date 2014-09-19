package io.lumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
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
    private User user;

    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.user = workerPrepareData.getUser();
    }

    public abstract void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception;

    protected File getLocalFileForRaw(Element element) throws IOException {
        StreamingPropertyValue rawValue = LumifyProperties.RAW.getPropertyValue(element);
        InputStream in = rawValue.getInputStream();
        try {
            File f = File.createTempFile(PostMimeTypeWorker.class.getName() + "-", "-" + element.getId());
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

    protected User getUser () { return user;}

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }
}
