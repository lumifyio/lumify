package io.lumify.palantir.dataImport;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

public abstract class PtRowImporterBase<T> extends PtImporterBase<T> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PtRowImporterBase.class);
    protected int count;

    protected PtRowImporterBase(DataImporter dataImporter, Class<T> ptClass) {
        super(dataImporter, ptClass);
    }

    private void handleProcessRowError(T row, Throwable ex) {
        LOGGER.error("Could not process row: %s (type: %s)", row, getPtClass().getSimpleName(), ex);
    }

    @Override
    protected long run(Iterable<T> rows) {
        count = 0;

        for (T row : rows) {
            if (count % 1000 == 0) {
                LOGGER.debug("Importing %s: %d", getPtClass().getSimpleName(), count);
                getDataImporter().getGraph().flush();
            }
            try {
                processRow(row);
            } catch (Throwable ex) {
                handleProcessRowError(row, ex);
            }
            count++;
        }

        return count;
    }

    protected abstract void processRow(T row) throws Exception;
}
