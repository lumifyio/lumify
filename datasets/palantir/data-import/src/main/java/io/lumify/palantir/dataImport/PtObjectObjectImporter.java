package io.lumify.palantir.dataImport;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import io.lumify.core.exception.LumifyException;
import io.lumify.palantir.dataImport.model.PtLinkType;
import io.lumify.palantir.dataImport.model.PtObjectObject;
import org.securegraph.Vertex;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class PtObjectObjectImporter extends PtGroupingImporterBase<PtObjectObject, Long> {
    protected PtObjectObjectImporter(final DataImporter dataImporter) {
        super(dataImporter, PtObjectObject.class);
    }

    @Override
    protected void beforeProcessRows() {
        super.beforeProcessRows();
        getDataImporter().getGraph().flush();
    }

    @Override
    protected void processGroup(Long parentObjectId, List<PtObjectObject> rows) throws Exception {
        String sourceObjectId = getObjectVertexId(parentObjectId);
        Vertex sourceVertex = getVertexCache().get(sourceObjectId);
        checkNotNull(sourceVertex, "Could not find source vertex: " + sourceObjectId);

        Iterable<String> destVertexIds = Iterables.transform(rows, new Function<PtObjectObject, String>() {
            @Override
            public String apply(PtObjectObject row) {
                return getObjectVertexId(row.getChildObjectId());
            }
        });
        Map<String, Vertex> destVertices = getVertexCacheVertices(destVertexIds);

        for (PtObjectObject row : rows) {
            PtLinkType ptLinkType = getDataImporter().getLinkTypes().get(row.getType());
            if (ptLinkType == null) {
                throw new LumifyException("Could not find link type: " + row.getType());
            }
            String linkTypeUri = getLinkTypeUri(ptLinkType.getUri());

            String destObjectId = getObjectVertexId(row.getChildObjectId());
            Vertex destVertex = destVertices.get(destObjectId);
            checkNotNull(destVertex, "Could not find dest vertex: " + destObjectId);

            String edgeId = getEdgeId(row);
            getDataImporter().getGraph().addEdge(edgeId, sourceVertex, destVertex, linkTypeUri, getDataImporter().getVisibility(), getDataImporter().getAuthorizations());
        }
    }

    @Override
    protected Long getGroupKey(PtObjectObject ptObjectObject) {
        return ptObjectObject.getParentObjectId();
    }

    protected String getEdgeId(PtObjectObject ptObjectObject) {
        return getDataImporter().getIdPrefix() + ptObjectObject.getLinkId();
    }

    protected String getLinkTypeUri(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
    }

    @Override
    protected String getSql() {
        // order by parent_object_id to improve cache hits
        return "select * from {namespace}.PT_OBJECT_OBJECT ORDER BY parent_object_id";
    }
}
