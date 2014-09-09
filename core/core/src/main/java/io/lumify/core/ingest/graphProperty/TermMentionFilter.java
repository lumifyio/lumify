package io.lumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;

public abstract class TermMentionFilter {
    private Configuration configuration;
    private Graph graph;

    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
    }

    public abstract Iterable<Vertex> apply(Vertex artifactGraphVertex, Iterable<Vertex> termMentions, Authorizations authorizations) throws Exception;

    @Inject
    public final void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    protected final Configuration getConfiguration() {
        return configuration;
    }

    protected final Graph getGraph() {
        return graph;
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }
}
