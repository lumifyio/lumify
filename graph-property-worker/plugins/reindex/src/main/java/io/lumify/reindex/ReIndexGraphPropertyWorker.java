package io.lumify.reindex;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import org.securegraph.Element;
import org.securegraph.GraphBase;
import org.securegraph.GraphBaseWithSearchIndex;
import org.securegraph.Property;
import org.securegraph.search.SearchIndex;

import java.io.InputStream;

public class ReIndexGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        if (getGraph() instanceof GraphBase) {
            SearchIndex searchIndex = ((GraphBaseWithSearchIndex) getGraph()).getSearchIndex();
            searchIndex.addElement(getGraph(), data.getElement(), data.getElement().getAuthorizations());
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return true;
        }

        return false;
    }
}
