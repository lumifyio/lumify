package com.altamiracorp.lumify.storm.file;

import com.altamiracorp.lumify.core.ingest.AdditionalArtifactWorkData;
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.ingest.TextExtractionWorkerPrepareData;
import com.altamiracorp.lumify.core.ingest.document.DocumentTextExtractionWorker;
import com.altamiracorp.lumify.core.ingest.image.ImageTextExtractionWorker;
import com.altamiracorp.lumify.core.ingest.structuredData.StructuredDataExtractionWorker;
import com.altamiracorp.lumify.core.ingest.video.VideoTextExtractionWorker;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import com.altamiracorp.lumify.core.util.ThreadedTeeInputStreamWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class HashCalculationWorker
        extends ThreadedTeeInputStreamWorker<ArtifactExtractedInfo, AdditionalArtifactWorkData>
        implements DocumentTextExtractionWorker, ImageTextExtractionWorker, VideoTextExtractionWorker, StructuredDataExtractionWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(HashCalculationWorker.class.getName());

    @Override
    protected ArtifactExtractedInfo doWork(InputStream work, AdditionalArtifactWorkData additionalArtifactWorkData) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Calculating Hash [HashCalculationWorker]: " + additionalArtifactWorkData.getFileName());
        }
        ArtifactExtractedInfo info = new ArtifactExtractedInfo();
        info.setRowKey(RowKeyHelper.buildSHA256KeyString(work));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Calculated hash: " + info.getRowKey());
            LOGGER.debug("Finished [HashCalculationWorker]: " + additionalArtifactWorkData.getFileName());
        }
        return info;
    }

    @Override
    public void prepare(TextExtractionWorkerPrepareData data) {
    }
}
