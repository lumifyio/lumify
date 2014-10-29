package io.lumify.palantir.dataImport;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.lumify.core.exception.LumifyException;
import io.lumify.palantir.dataImport.model.PtLinkType;
import io.lumify.palantir.dataImport.model.PtObjectObject;
import org.securegraph.Vertex;

import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

public class PtObjectObjectImporter extends PtImporterBase<PtObjectObject> {
    private final LoadingCache<String, Vertex> vertexCache;

    protected PtObjectObjectImporter(final DataImporter dataImporter) {
        super(dataImporter, PtObjectObject.class);

        vertexCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(new CacheLoader<String, Vertex>() {
                    @Override
                    public Vertex load(String key) throws Exception {
                        return dataImporter.getGraph().getVertex(key, dataImporter.getAuthorizations());
                    }
                });
    }

    @Override
    protected void beforeProcessRows() {
        super.beforeProcessRows();
        getDataImporter().getGraph().flush();
    }

    @Override
    protected void processRow(PtObjectObject row) throws ExecutionException {
        PtLinkType ptLinkType = getDataImporter().getLinkTypes().get(row.getType());
        if (ptLinkType == null) {
            throw new LumifyException("Could not find link type: " + row.getType());
        }
        String linkTypeUri = getLinkTypeUri(ptLinkType.getUri());

        String sourceObjectId = getObjectId(row.getParentObjectId());
        Vertex sourceVertex = vertexCache.get(sourceObjectId);
        checkNotNull(sourceVertex, "Could not find source vertex: " + sourceObjectId);

        String destObjectId = getObjectId(row.getChildObjectId());
        Vertex destVertex = vertexCache.get(destObjectId);
        checkNotNull(destVertex, "Could not find dest vertex: " + destObjectId);

        String edgeId = getEdgeId(row);
        getDataImporter().getGraph().addEdge(edgeId, sourceVertex, destVertex, linkTypeUri, getDataImporter().getVisibility(), getDataImporter().getAuthorizations());
    }

    @Override
    protected String getSql() {
        // order by parent_object_id to improve cache hits
        return "select * from {namespace}.PT_OBJECT_OBJECT ORDER BY parent_object_id";
    }
}
