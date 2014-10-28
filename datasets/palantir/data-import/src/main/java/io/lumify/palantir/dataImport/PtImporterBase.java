package io.lumify.palantir.dataImport;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.dataImport.model.PtObject;
import io.lumify.palantir.dataImport.model.PtObjectObject;
import io.lumify.palantir.dataImport.model.PtPropertyAndValue;

public abstract class PtImporterBase<T> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PtImporterBase.class);
    private final Class<T> ptClass;
    private final DataImporter dataImporter;
    private final static ValueEvaluator valueEvaluator = new ValueEvaluator();

    protected PtImporterBase(DataImporter dataImporter, Class<T> ptClass) {
        this.dataImporter = dataImporter;
        this.ptClass = ptClass;
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

    protected ValueEvaluator getValueEvaluator() {
        return valueEvaluator;
    }

    protected abstract void processRow(T row) throws Exception;

    protected abstract String getSql();

    protected String getConceptTypeUri(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
    }

    protected String getPropertyName(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
    }

    protected String getLinkTypeUri(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
    }

    protected String getEdgeId(PtObjectObject ptObjectObject) {
        return getDataImporter().getIdPrefix() + ptObjectObject.getLinkId();
    }

    protected String getObjectId(long objectId) {
        return getDataImporter().getIdPrefix() + objectId;
    }

    protected String getObjectId(PtPropertyAndValue ptPropertyAndValue) {
        return getDataImporter().getIdPrefix() + ptPropertyAndValue.getLinkObjectId();
    }

    protected String getObjectId(PtObject ptObject) {
        return getDataImporter().getIdPrefix() + ptObject.getObjectId();
    }


}
