package io.lumify.core.model.termMention;

import com.google.inject.Inject;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.security.LumifyVisibility;
import org.securegraph.*;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.util.FilterIterable;

import static org.securegraph.util.IterableUtils.singleOrDefault;

public class TermMentionRepository {
    public static final String VISIBILITY_STRING = "termMention";
    public static final String OWL_IRI = "http://lumify.io/termMention";
    private final Graph graph;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public TermMentionRepository(Graph graph, AuthorizationRepository authorizationRepository) {
        this.graph = graph;
        this.authorizationRepository = authorizationRepository;
        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
    }

    public Iterable<Vertex> findBySourceGraphVertexAndPropertyKey(String sourceVertexId, final String propertyKey, Authorizations authorizations) {
        authorizations = getAuthorizations(authorizations);
        return new FilterIterable<Vertex>(findBySourceGraphVertex(sourceVertexId, authorizations)) {
            @Override
            protected boolean isIncluded(Vertex v) {
                String vertexPropertyKey = LumifyProperties.TERM_MENTION_PROPERTY_KEY.getPropertyValue(v);
                return propertyKey.equals(vertexPropertyKey);
            }
        };
    }

    public Iterable<Vertex> findBySourceGraphVertex(String sourceVertexId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        Vertex sourceVertex = graph.getVertex(sourceVertexId, authorizationsWithTermMention);
        return sourceVertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizationsWithTermMention);
    }

    public Vertex findById(String termMentionId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        return graph.getVertex(termMentionId, authorizationsWithTermMention);
    }

    public void updateVisibility(Vertex termMention, Visibility newVisibility, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        newVisibility = LumifyVisibility.and(newVisibility, TermMentionRepository.VISIBILITY_STRING);
        ExistingElementMutation<Vertex> m = termMention.prepareMutation();
        m.alterElementVisibility(newVisibility);
        for (Property property : termMention.getProperties()) {
            m.alterPropertyVisibility(property, newVisibility);
        }
        m.save(authorizationsWithTermMention);
        for (Edge edge : termMention.getEdges(Direction.BOTH, authorizationsWithTermMention)) {
            ExistingElementMutation<Edge> edgeMutation = edge.prepareMutation();
            edgeMutation.alterElementVisibility(newVisibility);
            for (Property property : edge.getProperties()) {
                edgeMutation.alterPropertyVisibility(property, newVisibility);
            }
            edgeMutation.save(authorizationsWithTermMention);
        }
    }

    public Iterable<Vertex> findResolvedTo(String destVertexId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        Vertex destVertex = graph.getVertex(destVertexId, authorizationsWithTermMention);
        return destVertex.getVertices(Direction.IN, LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, authorizationsWithTermMention);
    }

    public void delete(Vertex termMention, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        graph.removeVertex(termMention, authorizationsWithTermMention);
    }

    public void markHidden(Vertex termMention, Visibility hiddenVisibility, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        graph.markVertexHidden(termMention, hiddenVisibility, authorizationsWithTermMention);
    }

    public Iterable<Vertex> findByEdgeId(String sourceVertexId, final String edgeId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        Vertex sourceVertex = graph.getVertex(sourceVertexId, authorizationsWithTermMention);
        return new FilterIterable<Vertex>(sourceVertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizationsWithTermMention)) {
            @Override
            protected boolean isIncluded(Vertex v) {
                String vertexEdgeId = LumifyProperties.TERM_MENTION_RESOLVED_EDGE_ID.getPropertyValue(v);
                return edgeId.equals(vertexEdgeId);
            }
        };
    }

    public Vertex findSourceVertex(Vertex termMention, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        return singleOrDefault(termMention.getVertices(Direction.IN, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizationsWithTermMention), null);
    }

    public Authorizations getAuthorizations(Authorizations authorizations) {
        return authorizationRepository.createAuthorizations(authorizations, TermMentionRepository.VISIBILITY_STRING);
    }
}
