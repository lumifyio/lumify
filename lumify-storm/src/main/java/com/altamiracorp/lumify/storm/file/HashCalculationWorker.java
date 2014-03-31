package com.altamiracorp.lumify.storm.file;

import com.altamiracorp.lumify.core.ingest.AdditionalArtifactWorkData;
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.ingest.TextExtractionWorkerPrepareData;
import com.altamiracorp.lumify.core.ingest.structuredData.StructuredDataExtractionWorker;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import com.altamiracorp.lumify.core.util.ThreadedTeeInputStreamWorker;

import java.io.InputStream;

public class HashCalculationWorker
        extends ThreadedTeeInputStreamWorker<ArtifactExtractedInfo, AdditionalArtifactWorkData>
        implements StructuredDataExtractionWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(HashCalculationWorker.class);

    @Override
    protected ArtifactExtractedInfo doWork(InputStream work, AdditionalArtifactWorkData additionalArtifactWorkData) throws Exception {
        LOGGER.debug("Calculating Hash [HashCalculationWorker]: %s", additionalArtifactWorkData.getFileName());
        ArtifactExtractedInfo info = new ArtifactExtractedInfo();
        info.setRowKey(RowKeyHelper.buildSHA256KeyString(work));
        LOGGER.debug("Calculated hash: %s", info.getRowKey());
        LOGGER.debug("Finished [HashCalculationWorker]: %s", additionalArtifactWorkData.getFileName());
        return info;
    }

    @Override
    public void prepare(TextExtractionWorkerPrepareData data) {
    }
}
