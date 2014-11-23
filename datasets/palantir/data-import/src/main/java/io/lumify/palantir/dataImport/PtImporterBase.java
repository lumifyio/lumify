package io.lumify.palantir.dataImport;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class PtImporterBase<T> implements Runnable {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PtImporterBase.class);
    private final Class<T> ptClass;
    private final DataImporter dataImporter;
    private final LoadingCache<String, Vertex> vertexCache;

    protected PtImporterBase(final DataImporter dataImporter, Class<T> ptClass) {
        this.dataImporter = dataImporter;
        this.ptClass = ptClass;

        vertexCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(new CacheLoader<String, Vertex>() {
                    @Override
                    public Vertex load(String key) throws Exception {
                        return dataImporter.getGraph().getVertex(key, dataImporter.getAuthorizations());
                    }
                });
    }

    public void run() {
        long startTime = System.currentTimeMillis();
        LOGGER.info(this.getClass().getName());
        Iterable<T> rows = dataImporter.getSqlRunner().select(getSql(), ptClass);
        beforeProcessRows();
        long count = run(rows);
        afterProcessRows();
        long endTime = System.currentTimeMillis();
        LOGGER.info("Imported %d %s (time: %dms)", count, this.ptClass.getSimpleName(), endTime - startTime);
    }

    protected abstract long run(Iterable<T> rows);

    protected void afterProcessRows() {

    }

    protected void beforeProcessRows() {

    }

    protected DataImporter getDataImporter() {
        return dataImporter;
    }

    protected abstract String getSql();

    public LoadingCache<String, Vertex> getVertexCache() {
        return vertexCache;
    }

    protected Map<String, Vertex> getVertexCacheVertices(Iterable<String> vertexIds) {
        List<String> vertexIdsToGet = new ArrayList<String>();
        List<Vertex> vertices = new ArrayList<Vertex>();

        // get the vertices already in the cache
        for (String vertexId : vertexIds) {
            Vertex v = this.vertexCache.getIfPresent(vertexId);
            if (v == null) {
                vertexIdsToGet.add(vertexId);
            } else {
                vertices.add(v);
            }
        }

        // get the vertices not in the cache
        Iterable<Vertex> newVertices = getDataImporter().getGraph().getVertices(vertexIdsToGet, getDataImporter().getAuthorizations());
        for (Vertex v : newVertices) {
            vertices.add(v);
            this.vertexCache.put(v.getId(), v);
        }

        // build a map to return
        return Maps.uniqueIndex(vertices, new Function<Vertex, String>() {
            @Override
            public String apply(Vertex input) {
                return input.getId();
            }
        });
    }

    protected String getObjectVertexId(long objectId) {
        return getDataImporter().getIdPrefix() + objectId;
    }

    public Class<T> getPtClass() {
        return ptClass;
    }
}
