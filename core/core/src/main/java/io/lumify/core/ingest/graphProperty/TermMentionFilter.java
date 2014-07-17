package io.lumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.ingest.term.extraction.TermMention;
import org.securegraph.Vertex;

public abstract class TermMentionFilter {
    private Configuration configuration;

    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
    }

    public abstract Iterable<TermMention> apply(Vertex artifactGraphVertex, Iterable<TermMention> termMentions) throws Exception;

    @Inject
    public final void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    protected Configuration getConfiguration() {
        return configuration;
    }
}
