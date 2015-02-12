package io.lumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
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
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PostMimeTypeWorker.class);
    private Graph graph;
    private WorkQueueRepository workQueueRepository;
    private File localFileForRaw;

    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {

    }

    protected abstract void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception;

    public void executeAndCleanup(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        try {
            execute(mimeType, data, authorizations);
        } catch (Throwable ex) {
            if (localFileForRaw != null) {
                if (!localFileForRaw.delete()) {
                    LOGGER.warn("Could not delete local file: %s", localFileForRaw.getAbsolutePath());
                }
                localFileForRaw = null;
            }
            throw ex;
        }
    }

    protected File getLocalFileForRaw(Element element) throws IOException {
        if (localFileForRaw != null) {
            return localFileForRaw;
        }
        StreamingPropertyValue rawValue = LumifyProperties.RAW.getPropertyValue(element);
        try (InputStream in = rawValue.getInputStream()) {
            localFileForRaw = File.createTempFile(PostMimeTypeWorker.class.getName() + "-", "-" + element.getId());
            try (FileOutputStream out = new FileOutputStream(localFileForRaw)) {
                IOUtils.copy(in, out);
                return localFileForRaw;
            }
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
