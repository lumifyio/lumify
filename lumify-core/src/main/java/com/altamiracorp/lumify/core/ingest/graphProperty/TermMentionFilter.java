package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.lumify.core.ingest.term.extraction.TermMention;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;

public abstract class TermMentionFilter {
    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
    }

    public abstract Iterable<TermMention> apply(Vertex artifactGraphVertex, Iterable<TermMention> termMentions, Visibility visibility) throws Exception;
}
