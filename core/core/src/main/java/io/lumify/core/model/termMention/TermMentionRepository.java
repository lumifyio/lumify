package io.lumify.core.model.termMention;

import com.google.inject.Inject;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.AuthorizationRepository;
import org.securegraph.*;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.util.FilterIterable;

import static org.securegraph.util.IterableUtils.singleOrDefault;

public class TermMentionRepository {
    public static final String VISIBILITY = "termMention";
    private final Graph graph;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public TermMentionRepository(Graph graph, AuthorizationRepository authorizationRepository) {
        this.graph = graph;
        this.authorizationRepository = authorizationRepository;
        authorizationRepository.addAuthorizationToGraph(TermMentionRepository.VISIBILITY);
    }

    public Iterable<Vertex> findBySourceGraphVertexAndPropertyKey(String sourceVertexId, final String propertyKey, Authorizations authorizations) {
        return new FilterIterable<Vertex>(findBySourceGraphVertex(sourceVertexId, authorizations)) {
            @Override
            protected boolean isIncluded(Vertex v) {
                String vertexPropertyKey = LumifyProperties.TERM_MENTION_PROPERTY_KEY.getPropertyValue(v);
                return propertyKey.equals(vertexPropertyKey);
            }
        };
    }

    public Iterable<Vertex> findBySourceGraphVertex(String sourceVertexId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = authorizationRepository.createAuthorizations(authorizations, TermMentionRepository.VISIBILITY);
        Vertex sourceVertex = graph.getVertex(sourceVertexId, authorizationsWithTermMention);
        return sourceVertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizationsWithTermMention);
    }

    public Vertex findById(String termMentionId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = authorizationRepository.createAuthorizations(authorizations, TermMentionRepository.VISIBILITY);
        return graph.getVertex(termMentionId, authorizationsWithTermMention);
    }

    public void updateVisibility(Vertex termMention, Visibility originalVisibility, Visibility newVisibility, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = authorizationRepository.createAuthorizations(authorizations, TermMentionRepository.VISIBILITY);
        ExistingElementMutation<Vertex> m = termMention.prepareMutation();
        m.alterElementVisibility(newVisibility);
        m.save(authorizationsWithTermMention);
    }

    public Iterable<Vertex> findResolvedTo(String destVertexId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = authorizationRepository.createAuthorizations(authorizations, TermMentionRepository.VISIBILITY);
        Vertex destVertex = graph.getVertex(destVertexId, authorizationsWithTermMention);
        return destVertex.getVertices(Direction.IN, LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, authorizationsWithTermMention);
    }

    public void delete(Vertex termMention, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = authorizationRepository.createAuthorizations(authorizations, TermMentionRepository.VISIBILITY);
        graph.removeVertex(termMention, authorizationsWithTermMention);
    }

    public Iterable<Vertex> findByEdgeId(String sourceVertexId, final String edgeId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = authorizationRepository.createAuthorizations(authorizations, TermMentionRepository.VISIBILITY);
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
        Authorizations authorizationsWithTermMention = authorizationRepository.createAuthorizations(authorizations, TermMentionRepository.VISIBILITY);
        return singleOrDefault(termMention.getVertices(Direction.IN, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizationsWithTermMention), null);
    }
}
