package io.lumify.core.model.termMention;

import com.google.inject.Inject;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.*;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.util.FilterIterable;

import static org.securegraph.util.IterableUtils.singleOrDefault;

public class TermMentionRepository {
    public static final String VISIBILITY = "termMention";
    private final Graph graph;

    @Inject
    public TermMentionRepository(Graph graph) {
        this.graph = graph;
    }

    public Iterable<Vertex> findBySourceGraphVertexAndPropertyKey(Vertex sourceVertex, final String propertyKey, Authorizations authorizations) {
        return new FilterIterable<Vertex>(sourceVertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizations)) {
            @Override
            protected boolean isIncluded(Vertex v) {
                String vertexPropertyKey = LumifyProperties.TERM_MENTION_PROPERTY_KEY.getPropertyValue(v);
                return propertyKey.equals(vertexPropertyKey);
            }
        };
    }

    public Vertex findById(String termMentionId, Authorizations authorizations) {
        return graph.getVertex(termMentionId, authorizations);
    }

    public void updateVisibility(Vertex termMention, Visibility originalVisibility, Visibility newVisibility, Authorizations authorizations) {
        ExistingElementMutation<Vertex> m = termMention.prepareMutation();
        m.alterElementVisibility(newVisibility);
        m.save(authorizations);
    }

    public Iterable<Vertex> findResolvedTo(Vertex destVertex, Authorizations authorizations) {
        return destVertex.getVertices(Direction.IN, LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, authorizations);
    }

    public void delete(Vertex termMention, Authorizations authorizations) {
        graph.removeVertex(termMention, authorizations);
    }

    public Iterable<Vertex> findByEdgeId(Vertex sourceVertex, final String edgeId, Authorizations authorizations) {
        return new FilterIterable<Vertex>(sourceVertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizations)) {
            @Override
            protected boolean isIncluded(Vertex v) {
                String vertexEdgeId = LumifyProperties.TERM_MENTION_RESOLVED_EDGE_ID.getPropertyValue(v);
                return edgeId.equals(vertexEdgeId);
            }
        };
    }

    public Vertex findSourceVertex(Vertex termMention, Authorizations authorizations) {
        return singleOrDefault(termMention.getVertices(Direction.IN, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizations), null);
    }
}
