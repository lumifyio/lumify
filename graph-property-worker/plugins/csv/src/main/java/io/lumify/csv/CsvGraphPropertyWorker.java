package io.lumify.csv;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.mapping.AbstractDocumentMappingGraphPropertyWorker;
import io.lumify.mapping.csv.CsvDocumentMapping;

public class CsvGraphPropertyWorker extends AbstractDocumentMappingGraphPropertyWorker<CsvDocumentMapping> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(CsvGraphPropertyWorker.class);
    private static final String MULTI_KEY = CsvGraphPropertyWorker.class.getName();
    private static final String CONCEPT_IRI_INTENT = "csv";
    public static final String VERTEX_ID_PREFIX = "CSV_";

    public CsvGraphPropertyWorker() {
        super(CsvDocumentMapping.class, MULTI_KEY);
    }

    @Override
    protected String getConceptIriIntent() {
        return CONCEPT_IRI_INTENT;
    }

    @Override
    protected String getVertexIdPrefix() {
        return VERTEX_ID_PREFIX;
    }
}
