package io.lumify.core.ingest.graphProperty;

import io.lumify.core.ingest.term.extraction.TermMention;
import org.securegraph.Vertex;

public abstract class TermMentionFilter {
    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
    }

    public abstract Iterable<TermMention> apply(Vertex artifactGraphVertex, Iterable<TermMention> termMentions) throws Exception;
}
