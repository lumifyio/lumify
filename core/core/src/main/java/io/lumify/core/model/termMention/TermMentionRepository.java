package io.lumify.core.model.termMention;

import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Authorizations;
import org.securegraph.Direction;
import org.securegraph.Vertex;
import org.securegraph.util.FilterIterable;

public class TermMentionRepository {
    public Iterable<Vertex> findBySourceGraphVertexAndPropertyKey(Vertex sourceVertex, final String propertyKey, Authorizations authorizations) {
        return new FilterIterable<Vertex>(sourceVertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizations)) {
            @Override
            protected boolean isIncluded(Vertex v) {
                String vertexPropertyKey = LumifyProperties.TERM_MENTION_PROPERTY_KEY.getPropertyValue(v);
                return propertyKey.equals(vertexPropertyKey);
            }
        };
    }
}
