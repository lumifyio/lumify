package io.lumify.core.model.termMention;

import com.google.inject.Inject;
import io.lumify.core.model.PropertyJustificationMetadata;
import io.lumify.core.model.SourceInfo;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.*;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.util.FilterIterable;
import org.securegraph.util.JoinIterable;

import static org.securegraph.util.IterableUtils.single;
import static org.securegraph.util.IterableUtils.singleOrDefault;

public class TermMentionRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TermMentionRepository.class);
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

    /**
     * Find all term mentions connected to the vertex.
     */
    public Iterable<Vertex> findByVertexId(String vertexId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        Vertex vertex = graph.getVertex(vertexId, authorizationsWithTermMention);
        String[] labels = new String[]{
                LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION,
                LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO
        };
        return vertex.getVertices(Direction.BOTH, labels, authorizationsWithTermMention);
    }

    /**
     * Find all term mentions connected to either side of the edge.
     */
    public Iterable<Vertex> findByEdge(Edge edge, Authorizations authorizations) {
        return new JoinIterable<>(
                findByVertexId(edge.getVertexId(Direction.IN), authorizations),
                findByVertexId(edge.getVertexId(Direction.OUT), authorizations)
        );
    }

    /**
     * Finds term mention vertices that were created for the justification of a new vertex.
     *
     * @param vertexId The vertex id of the vertex with the justification.
     * @return term mention vertices matching the criteria.
     */
    public Iterable<Vertex> findByVertexIdForVertex(final String vertexId, Authorizations authorizations) {
        return new FilterIterable<Vertex>(findByVertexId(vertexId, authorizations)) {
            @Override
            protected boolean isIncluded(Vertex termMention) {
                String forElementId = LumifyProperties.TERM_MENTION_FOR_ELEMENT_ID.getPropertyValue(termMention);
                if (forElementId == null || !forElementId.equals(vertexId)) {
                    return false;
                }

                TermMentionFor forType = LumifyProperties.TERM_MENTION_FOR_TYPE.getPropertyValue(termMention);
                if (forType == null || forType != TermMentionFor.VERTEX) {
                    return false;
                }

                return true;
            }
        };
    }

    /**
     * Finds term mention vertices that were created for the justification of a new edge.
     *
     * @param edge The edge id of the edge with the justification.
     * @return term mention vertices matching the criteria.
     */
    public Iterable<Vertex> findByEdgeForEdge(final Edge edge, Authorizations authorizations) {
        return new FilterIterable<Vertex>(findByEdge(edge, authorizations)) {
            @Override
            protected boolean isIncluded(Vertex termMention) {
                String forElementId = LumifyProperties.TERM_MENTION_FOR_ELEMENT_ID.getPropertyValue(termMention);
                if (forElementId == null || !forElementId.equals(edge.getId())) {
                    return false;
                }

                TermMentionFor forType = LumifyProperties.TERM_MENTION_FOR_TYPE.getPropertyValue(termMention);
                if (forType == null || forType != TermMentionFor.EDGE) {
                    return false;
                }

                return true;
            }
        };
    }

    /**
     * Finds all term mentions connected to a vertex that match propertyKey, propertyName, and propertyVisibility.
     */
    public Iterable<Vertex> findByVertexIdAndProperty(final String vertexId, final String propertyKey, final String propertyName, final Visibility propertyVisibility, Authorizations authorizations) {
        return new FilterIterable<Vertex>(findByVertexId(vertexId, authorizations)) {
            @Override
            protected boolean isIncluded(Vertex termMention) {
                String forElementId = LumifyProperties.TERM_MENTION_FOR_ELEMENT_ID.getPropertyValue(termMention);
                if (forElementId == null || !forElementId.equals(vertexId)) {
                    return false;
                }
                return isTermMentionForProperty(termMention, propertyKey, propertyName, propertyVisibility);
            }
        };
    }

    /**
     * Finds all term mentions connected to either side of an edge that match propertyKey, propertyName, and propertyVisibility.
     */
    public Iterable<Vertex> findByEdgeIdAndProperty(final Edge edge, final String propertyKey, final String propertyName, final Visibility propertyVisibility, Authorizations authorizations) {
        return new FilterIterable<Vertex>(findByEdge(edge, authorizations)) {
            @Override
            protected boolean isIncluded(Vertex termMention) {
                String forElementId = LumifyProperties.TERM_MENTION_FOR_ELEMENT_ID.getPropertyValue(termMention);
                if (forElementId == null || !forElementId.equals(edge.getId())) {
                    return false;
                }
                return isTermMentionForProperty(termMention, propertyKey, propertyName, propertyVisibility);
            }
        };
    }

    private boolean isTermMentionForProperty(Vertex termMention, String propertyKey, String propertyName, Visibility propertyVisibility) {
        TermMentionFor forType = LumifyProperties.TERM_MENTION_FOR_TYPE.getPropertyValue(termMention);
        if (forType == null || forType != TermMentionFor.PROPERTY) {
            return false;
        }

        String refPropertyKey = LumifyProperties.TERM_MENTION_REF_PROPERTY_KEY.getPropertyValue(termMention);
        if (refPropertyKey == null || !refPropertyKey.equals(propertyKey)) {
            return false;
        }

        String refPropertyName = LumifyProperties.TERM_MENTION_REF_PROPERTY_NAME.getPropertyValue(termMention);
        if (refPropertyName == null || !refPropertyName.equals(propertyName)) {
            return false;
        }

        String refPropertyVisibilityString = LumifyProperties.TERM_MENTION_REF_PROPERTY_VISIBILITY.getPropertyValue(termMention);
        if (refPropertyVisibilityString == null || !refPropertyVisibilityString.equals(propertyVisibility.getVisibilityString())) {
            return false;
        }

        return true;
    }

    public Vertex findById(String termMentionId, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        return graph.getVertex(termMentionId, authorizationsWithTermMention);
    }

    public void updateVisibility(Vertex termMention, Visibility newVisibility, Authorizations authorizations) {
        Authorizations authorizationsWithTermMention = getAuthorizations(authorizations);
        Visibility newVisibilityWithTermMention = LumifyVisibility.and(newVisibility, VISIBILITY_STRING);
        ExistingElementMutation<Vertex> m = termMention.prepareMutation();
        m.alterElementVisibility(newVisibilityWithTermMention);
        for (Property property : termMention.getProperties()) {
            m.alterPropertyVisibility(property, newVisibilityWithTermMention);
        }
        Property refPropertyVisibility = LumifyProperties.TERM_MENTION_REF_PROPERTY_VISIBILITY.getProperty(termMention);
        if (refPropertyVisibility != null) {
            LumifyProperties.TERM_MENTION_REF_PROPERTY_VISIBILITY.setProperty(m, newVisibility.getVisibilityString(), refPropertyVisibility.getMetadata(), newVisibilityWithTermMention);
        }
        m.save(authorizationsWithTermMention);
        for (Edge edge : termMention.getEdges(Direction.BOTH, authorizationsWithTermMention)) {
            ExistingElementMutation<Edge> edgeMutation = edge.prepareMutation();
            edgeMutation.alterElementVisibility(newVisibilityWithTermMention);
            for (Property property : edge.getProperties()) {
                edgeMutation.alterPropertyVisibility(property, newVisibilityWithTermMention);
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
        return authorizationRepository.createAuthorizations(authorizations, VISIBILITY_STRING);
    }

    public void addJustification(
            Vertex vertex,
            String justificationText,
            SourceInfo sourceInfo,
            LumifyVisibility lumifyVisibility,
            Authorizations authorizations
    ) {
        if (justificationText != null) {
            PropertyJustificationMetadata propertyJustificationMetadata = new PropertyJustificationMetadata(justificationText);
            removeSourceInfoEdgeFromVertex(vertex.getId(), vertex.getId(), null, null, lumifyVisibility, authorizations);
            LumifyProperties.JUSTIFICATION.setProperty(vertex, propertyJustificationMetadata, lumifyVisibility.getVisibility(), authorizations);
        } else if (sourceInfo != null) {
            Vertex sourceVertex = graph.getVertex(sourceInfo.getVertexId(), authorizations);
            LumifyProperties.JUSTIFICATION.removeProperty(vertex, authorizations);
            addSourceInfoToVertex(
                    vertex,
                    sourceInfo.getVertexId(),
                    TermMentionFor.VERTEX,
                    null,
                    null,
                    null,
                    sourceInfo.getSnippet(),
                    sourceInfo.getTextPropertyKey(),
                    sourceInfo.getStartOffset(),
                    sourceInfo.getEndOffset(),
                    sourceVertex,
                    lumifyVisibility.getVisibility(),
                    authorizations
            );
        }
    }

    public <T extends Element> void addSourceInfo(
            T element,
            String forElementId,
            TermMentionFor forType,
            String propertyKey,
            String propertyName,
            Visibility propertyVisibility,
            String snippet,
            String textPropertyKey,
            long startOffset,
            long endOffset,
            Vertex sourceVertex,
            Visibility visibility,
            Authorizations authorizations
    ) {
        if (element instanceof Vertex) {
            addSourceInfoToVertex(
                    (Vertex) element,
                    forElementId,
                    forType,
                    propertyKey,
                    propertyName,
                    propertyVisibility,
                    snippet,
                    textPropertyKey,
                    startOffset,
                    endOffset,
                    sourceVertex,
                    visibility,
                    authorizations
            );
        } else {
            addSourceInfoEdgeToEdge(
                    (Edge) element,
                    forElementId,
                    forType,
                    propertyKey,
                    propertyName,
                    propertyVisibility,
                    snippet,
                    textPropertyKey,
                    startOffset,
                    endOffset,
                    sourceVertex,
                    visibility,
                    authorizations
            );
        }
    }

    public void addSourceInfoToVertex(
            Vertex vertex,
            String forElementId,
            TermMentionFor forType,
            String propertyKey,
            String propertyName,
            Visibility propertyVisibility,
            String snippet,
            String textPropertyKey,
            long startOffset,
            long endOffset,
            Vertex sourceVertex,
            Visibility visibility,
            Authorizations authorizations
    ) {
        visibility = LumifyVisibility.and(visibility, VISIBILITY_STRING);
        String termMentionVertexId = vertex.getId() + "hasSource" + sourceVertex.getId();
        if (propertyKey != null) {
            termMentionVertexId += ":" + propertyKey;
        }
        if (propertyName != null) {
            termMentionVertexId += ":" + propertyName;
        }
        if (propertyVisibility != null) {
            termMentionVertexId += ":" + propertyVisibility;
        }
        VertexBuilder m = graph.prepareVertex(termMentionVertexId, visibility);
        LumifyProperties.TERM_MENTION_FOR_ELEMENT_ID.setProperty(m, forElementId, visibility);
        LumifyProperties.TERM_MENTION_FOR_TYPE.setProperty(m, forType, visibility);
        if (propertyKey != null) {
            LumifyProperties.TERM_MENTION_REF_PROPERTY_KEY.setProperty(m, propertyKey, visibility);
        }
        if (propertyName != null) {
            LumifyProperties.TERM_MENTION_REF_PROPERTY_NAME.setProperty(m, propertyName, visibility);
        }
        if (propertyVisibility != null) {
            LumifyProperties.TERM_MENTION_REF_PROPERTY_VISIBILITY.setProperty(m, propertyVisibility.getVisibilityString(), visibility);
        }
        LumifyProperties.TERM_MENTION_SNIPPET.setProperty(m, snippet, visibility);
        LumifyProperties.TERM_MENTION_PROPERTY_KEY.setProperty(m, textPropertyKey, visibility);
        LumifyProperties.TERM_MENTION_START_OFFSET.setProperty(m, startOffset, visibility);
        LumifyProperties.TERM_MENTION_END_OFFSET.setProperty(m, endOffset, visibility);
        Vertex termMention = m.save(authorizations);

        graph.addEdge(LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION + termMentionVertexId, sourceVertex, termMention, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, visibility, authorizations);
        graph.addEdge(LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO + termMentionVertexId, termMention, vertex, LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, visibility, authorizations);

        graph.flush();
        LOGGER.debug("added source info: %s", termMention.getId());
    }

    public void addSourceInfoEdgeToEdge(
            Edge edge,
            String forElementId,
            TermMentionFor forType,
            String propertyKey,
            String propertyName,
            Visibility propertyVisibility,
            String snippet,
            String textPropertyKey,
            long startOffset,
            long endOffset,
            Vertex sourceVertex,
            Visibility visibility,
            Authorizations authorizations
    ) {
        Vertex inVertex = edge.getVertex(Direction.IN, authorizations);
        Vertex outVertex = edge.getVertex(Direction.OUT, authorizations);
        addSourceInfoToVertex(
                inVertex,
                forElementId,
                forType,
                propertyKey,
                propertyName,
                propertyVisibility,
                snippet,
                textPropertyKey,
                startOffset,
                endOffset,
                sourceVertex,
                visibility,
                authorizations
        );
        addSourceInfoToVertex(
                outVertex,
                forElementId,
                forType,
                propertyKey,
                propertyName,
                propertyVisibility,
                snippet,
                textPropertyKey,
                startOffset,
                endOffset,
                sourceVertex,
                visibility,
                authorizations
        );
    }

    public void removeSourceInfoEdge(Element element, String propertyKey, String propertyName, LumifyVisibility lumifyVisibility, Authorizations authorizations) {
        if (element instanceof Vertex) {
            removeSourceInfoEdgeFromVertex(element.getId(), element.getId(), propertyKey, propertyName, lumifyVisibility, authorizations);
        } else {
            removeSourceInfoEdgeFromEdge((Edge) element, propertyKey, propertyName, lumifyVisibility, authorizations);
        }
    }

    public void removeSourceInfoEdgeFromVertex(String vertexId, String sourceInfoElementId, String propertyKey, String propertyName, LumifyVisibility lumifyVisibility, Authorizations authorizations) {
        Vertex termMention = findTermMention(vertexId, sourceInfoElementId, propertyKey, propertyName, lumifyVisibility.getVisibility(), authorizations);
        if (termMention != null) {
            graph.removeVertex(termMention, authorizations);
        }
    }

    public void removeSourceInfoEdgeFromEdge(Edge edge, String propertyKey, String propertyName, LumifyVisibility lumifyVisibility, Authorizations authorizations) {
        String inVertexId = edge.getVertexId(Direction.IN);
        String outVertexId = edge.getVertexId(Direction.OUT);
        removeSourceInfoEdgeFromVertex(inVertexId, edge.getId(), propertyKey, propertyName, lumifyVisibility, authorizations);
        removeSourceInfoEdgeFromVertex(outVertexId, edge.getId(), propertyKey, propertyName, lumifyVisibility, authorizations);
    }

    private Vertex findTermMention(String vertexId, String forElementId, String propertyKey, String propertyName, Visibility propertyVisibility, Authorizations authorizations) {
        Authorizations authorizationsWithTermMentions = getAuthorizations(authorizations);
        Vertex vertex = graph.getVertex(vertexId, authorizationsWithTermMentions);
        Iterable<Vertex> termMentions = vertex.getVertices(Direction.IN, LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, authorizationsWithTermMentions);
        for (Vertex termMention : termMentions) {
            if (forElementId != null && !forElementId.equals(LumifyProperties.TERM_MENTION_FOR_ELEMENT_ID.getPropertyValue(termMention))) {
                continue;
            }
            if (propertyKey != null && !propertyKey.equals(LumifyProperties.TERM_MENTION_REF_PROPERTY_KEY.getPropertyValue(termMention))) {
                continue;
            }
            if (propertyName != null && !propertyName.equals(LumifyProperties.TERM_MENTION_REF_PROPERTY_NAME.getPropertyValue(termMention))) {
                continue;
            }
            if (propertyVisibility != null && !propertyVisibility.toString().equals(LumifyProperties.TERM_MENTION_REF_PROPERTY_VISIBILITY.getPropertyValue(termMention))) {
                continue;
            }
            return termMention;
        }
        return null;
    }

    public SourceInfo getSourceInfoForEdge(Edge edge, Authorizations authorizations) {
        String inVertexId = edge.getVertexId(Direction.IN);
        Vertex termMention = findTermMention(inVertexId, edge.getId(), null, null, null, authorizations);
        return getSourceInfoFromTermMention(termMention, authorizations);
    }

    public SourceInfo getSourceInfoForVertex(Vertex vertex, Authorizations authorizations) {
        Vertex termMention = findTermMention(vertex.getId(), vertex.getId(), null, null, null, authorizations);
        return getSourceInfoFromTermMention(termMention, authorizations);
    }

    public SourceInfo getSourceInfoForEdgeProperty(Edge edge, String propertyKey, String propertyName, Visibility visibility, Authorizations authorizations) {
        String inVertexId = edge.getVertexId(Direction.IN);
        Vertex termMention = findTermMention(inVertexId, edge.getId(), propertyKey, propertyName, visibility, authorizations);
        return getSourceInfoFromTermMention(termMention, authorizations);
    }

    public SourceInfo getSourceInfoForVertexProperty(String vertexId, Property property, Authorizations authorizations) {
        Vertex termMention = findTermMention(vertexId, vertexId, property.getKey(), property.getName(), property.getVisibility(), authorizations);
        return getSourceInfoFromTermMention(termMention, authorizations);
    }

    private SourceInfo getSourceInfoFromTermMention(Vertex termMention, Authorizations authorizations) {
        if (termMention == null) {
            return null;
        }
        String vertexId = single(termMention.getVertexIds(Direction.IN, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizations));
        String textPropertyKey = LumifyProperties.TERM_MENTION_PROPERTY_KEY.getPropertyValue(termMention);
        long startOffset = LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(termMention);
        long endOffset = LumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(termMention);
        String snippet = LumifyProperties.TERM_MENTION_SNIPPET.getPropertyValue(termMention);

        return new SourceInfo(vertexId, textPropertyKey, startOffset, endOffset, snippet);
    }
}
