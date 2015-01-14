package io.lumify.tools;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.PostMimeTypeWorker;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.securegraph.mutation.ExistingElementMutation;

public class RdfPostMimeTypeWorker extends PostMimeTypeWorker {
    private static final String MULTI_KEY = RdfPostMimeTypeWorker.class.getName();

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (RdfOntology.MIME_TYPE_TEXT_RDF.equals(mimeType)) {
            addRdfGraphPropertyWorkerToWhiteList((Vertex) data.getElement(), data.getVisibility(), authorizations);
        }
    }

    private void addRdfGraphPropertyWorkerToWhiteList(Vertex vertex, Visibility visibility, Authorizations authorizations) {
        ExistingElementMutation<Vertex> m = vertex.prepareMutation();
        LumifyProperties.GRAPH_PROPERTY_WORKER_WHITE_LIST.addPropertyValue(m, MULTI_KEY, RdfGraphPropertyWorker.class.getName(), visibility);
        m.save(authorizations);
    }
}
