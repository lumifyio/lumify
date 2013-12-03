package com.altamiracorp.lumify.core.ingest.term.extraction;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.ThreadedTeeInputStreamWorker;

import java.util.Map;

public abstract class TermExtractionWorker extends ThreadedTeeInputStreamWorker<TermExtractionResult, TermExtractionAdditionalWorkData> {
    public abstract void prepare(Map conf, User user) throws Exception;
}
