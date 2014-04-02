package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.lumify.core.ingest.term.extraction.TermMention;
import com.altamiracorp.securegraph.Vertex;

public abstract class TermMentionFilter {
    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
    }

    public abstract Iterable<TermMention> apply(Vertex artifactGraphVertex, Iterable<TermMention> termMentions) throws Exception;
}
