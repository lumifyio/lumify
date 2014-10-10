package io.lumify.csv;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.PostMimeTypeWorker;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.securegraph.mutation.ExistingElementMutation;

public class CsvPostMimeTypeWorker extends PostMimeTypeWorker {
    private static final String MIME_TYPE_TEXT_CSV = "text/csv";
    private static final String MULTI_KEY = CsvPostMimeTypeWorker.class.getName();

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (MIME_TYPE_TEXT_CSV.equals(mimeType)) {
            addCsvGraphPropertyWorkerToWhiteList((Vertex) data.getElement(), data.getVisibility(), authorizations);
        }
    }

    private void addCsvGraphPropertyWorkerToWhiteList(Vertex vertex, Visibility visibility, Authorizations authorizations) {
        ExistingElementMutation<Vertex> m = vertex.prepareMutation();
        LumifyProperties.GRAPH_PROPERTY_WORKER_WHITE_LIST.addPropertyValue(m, MULTI_KEY, CsvGraphPropertyWorker.class.getName(), visibility);
        m.save(authorizations);
    }
}
