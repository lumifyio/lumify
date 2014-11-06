package io.lumify.palantir.dataImport;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class PtGroupingImporterBase<TRow, TGroupKey> extends PtImporterBase<TRow> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PtGroupingImporterBase.class);

    protected PtGroupingImporterBase(DataImporter dataImporter, Class<TRow> ptClass) {
        super(dataImporter, ptClass);
    }

    @Override
    protected long run(Iterable<TRow> rows) {
        int count = 0;
        TGroupKey lastGroupKey = null;
        List<TRow> groupRows = new ArrayList<TRow>();

        for (TRow row : rows) {
            if (count % 1000 == 0) {
                LOGGER.debug("Importing %s: %d", getPtClass().getSimpleName(), count);
                getDataImporter().getGraph().flush();
            }

            TGroupKey groupKey = getGroupKey(row);
            if (count == 0 || groupKeyEqual(groupKey, lastGroupKey)) {
                if (count == 0) {
                    lastGroupKey = groupKey;
                }
                groupRows.add(row);
            } else {
                processGroupInternal(lastGroupKey, groupRows);
                groupRows.clear();
                lastGroupKey = groupKey;
                groupRows.add(row);
            }

            count++;
        }

        if (groupRows.size() > 0) {
            processGroupInternal(lastGroupKey, groupRows);
        }

        return count;
    }

    private void processGroupInternal(TGroupKey lastGroupKey, List<TRow> groupRows) {
        try {
            processGroup(lastGroupKey, groupRows);
        } catch (Exception ex) {
            handleProcessGroupError(lastGroupKey, groupRows, ex);
        }
    }

    private void handleProcessGroupError(TGroupKey lastGroupKey, List<TRow> groupRows, Exception ex) {
        LOGGER.error("Could not process group: %s", lastGroupKey, ex);
    }

    protected boolean groupKeyEqual(TGroupKey groupKey, TGroupKey lastGroupKey) {
        if (groupKey == null && lastGroupKey == null) {
            return true;
        }
        if (groupKey == null) {
            return false;
        }
        return groupKey.equals(lastGroupKey);
    }

    protected abstract void processGroup(TGroupKey groupKey, List<TRow> rows) throws Exception;

    protected abstract TGroupKey getGroupKey(TRow row);
}
