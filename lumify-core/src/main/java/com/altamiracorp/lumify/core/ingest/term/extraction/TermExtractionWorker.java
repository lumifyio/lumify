package com.altamiracorp.lumify.core.ingest.term.extraction;

import com.altamiracorp.lumify.core.util.ThreadedTeeInputStreamWorker;

public abstract class TermExtractionWorker extends ThreadedTeeInputStreamWorker<TermExtractionResult, TermExtractionAdditionalWorkData>
    implements TermWorker {
}
