package io.lumify.palantir.dataImport;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Vertex;

public abstract class PtImporterBase<T> {
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
        int count = 0;
        LOGGER.info(this.getClass().getName());
        Iterable<T> rows = dataImporter.getSqlRunner().select(getSql(), ptClass);
        beforeProcessRows();
        for (T row : rows) {
            if (count % 1000 == 0) {
                LOGGER.debug("Importing %s: %d", this.ptClass.getSimpleName(), count);
                dataImporter.getGraph().flush();
            }

            try {
                processRow(row);
            } catch (Throwable ex) {
                handleProcessRowError(row, ex);
            }
            count++;
        }
        afterProcessRows();
        LOGGER.info("Imported %d %s", count, this.ptClass.getSimpleName());
    }

    private void handleProcessRowError(T row, Throwable ex) {
        LOGGER.error("Could not process row: %s (type: %s)", row, ptClass.getSimpleName(), ex);
    }

    protected void afterProcessRows() {

    }

    protected void beforeProcessRows() {

    }

    protected DataImporter getDataImporter() {
        return dataImporter;
    }

    protected abstract void processRow(T row) throws Exception;

    protected abstract String getSql();

    public LoadingCache<String, Vertex> getVertexCache() {
        return vertexCache;
    }

    protected String getObjectVertexId(long objectId) {
        return getDataImporter().getIdPrefix() + objectId;
    }
}
