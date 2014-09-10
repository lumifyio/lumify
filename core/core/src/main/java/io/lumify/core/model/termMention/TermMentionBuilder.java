package io.lumify.core.model.termMention;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import org.json.JSONObject;
import org.securegraph.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class TermMentionBuilder {
    private static final String TERM_MENTION_VERTEX_ID_PREFIX = "TM_";
    private Vertex sourceVertex;
    private String propertyKey;
    private long start = -1;
    private long end = -1;
    private String title;
    private String conceptIri;
    private JSONObject visibilitySource;
    private String process;
    private Vertex resolvedToVertex;
    private Edge resolvedEdge;

    public TermMentionBuilder() {

    }

    public TermMentionBuilder(Vertex existingTermMention, Vertex sourceVertex) {
        this.sourceVertex = sourceVertex;
        this.propertyKey = LumifyProperties.TERM_MENTION_PROPERTY_KEY.getPropertyValue(existingTermMention);
        this.start = LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(existingTermMention, 0);
        this.end = LumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(existingTermMention, 0);
        this.title = LumifyProperties.TITLE.getPropertyValue(existingTermMention, "");
        this.conceptIri = LumifyProperties.CONCEPT_TYPE.getPropertyValue(existingTermMention, "");
        this.visibilitySource = LumifyProperties.VISIBILITY_SOURCE.getPropertyValue(existingTermMention, "");
    }

    public TermMentionBuilder start(long start) {
        this.start = start;
        return this;
    }

    public TermMentionBuilder end(long end) {
        this.end = end;
        return this;
    }

    public TermMentionBuilder propertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
        return this;
    }

    public TermMentionBuilder visibilitySource(String visibilitySource) {
        return visibilitySource(visibilitySourceStringToJson(visibilitySource));
    }

    public TermMentionBuilder visibilitySource(JSONObject visibilitySource) {
        this.visibilitySource = visibilitySource;
        return this;
    }

    private static JSONObject visibilitySourceStringToJson(String visibilitySource) {
        if (visibilitySource == null) {
            return new JSONObject();
        }
        if (visibilitySource.length() == 0) {
            return new JSONObject();
        }
        return new JSONObject(visibilitySource);
    }

    public TermMentionBuilder resolvedTo(Vertex resolvedToVertex, Edge resolvedEdge) {
        this.resolvedToVertex = resolvedToVertex;
        this.resolvedEdge = resolvedEdge;
        return this;
    }

    public TermMentionBuilder process(String process) {
        this.process = process;
        return this;
    }

    public TermMentionBuilder sourceVertex(Vertex sourceVertex) {
        this.sourceVertex = sourceVertex;
        return this;
    }

    public TermMentionBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TermMentionBuilder conceptIri(String conceptIri) {
        this.conceptIri = conceptIri;
        return this;
    }

    public Vertex save(Graph graph, VisibilityTranslator visibilityTranslator, Authorizations authorizations) {
        checkNotNull(sourceVertex, "sourceVertex cannot be null");
        checkNotNull(propertyKey, "propertyKey cannot be null");
        checkNotNull(title, "title cannot be null");
        checkNotNull(conceptIri, "conceptIri cannot be null");
        checkNotNull(visibilitySource, "visibilitySource cannot be null");
        checkArgument(start >= 0, "start must be greater than or equal to 0");
        checkArgument(end >= 0, "start must be greater than or equal to 0");

        String vertexId = createVertexId();
        Visibility visibility = LumifyVisibility.and(visibilityTranslator.toVisibility(this.visibilitySource).getVisibility(), TermMentionRepository.VISIBILITY);
        VertexBuilder vertexBuilder = graph.prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, this.conceptIri, visibility);
        LumifyProperties.TITLE.setProperty(vertexBuilder, this.title, visibility);
        LumifyProperties.TERM_MENTION_START_OFFSET.setProperty(vertexBuilder, this.start, visibility);
        LumifyProperties.TERM_MENTION_END_OFFSET.setProperty(vertexBuilder, this.end, visibility);
        if (this.process != null) {
            LumifyProperties.TERM_MENTION_PROCESS.setProperty(vertexBuilder, this.process, visibility);
        }
        if (this.propertyKey != null) {
            LumifyProperties.TERM_MENTION_PROPERTY_KEY.setProperty(vertexBuilder, this.propertyKey, visibility);
        }
        if (this.resolvedEdge != null) {
            LumifyProperties.TERM_MENTION_RESOLVED_EDGE_ID.setProperty(vertexBuilder, this.resolvedEdge.getId(), visibility);
        }
        Vertex termMentionVertex = vertexBuilder.save(authorizations);

        String hasTermMentionId = vertexId + "_hasTermMention";
        graph.addEdge(hasTermMentionId, this.sourceVertex, termMentionVertex, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, visibility, authorizations);
        if (this.resolvedToVertex != null) {
            String resolvedToId = vertexId + "_resolvedTo";
            graph.addEdge(resolvedToId, termMentionVertex, this.resolvedToVertex, LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, visibility, authorizations);
        }

        return termMentionVertex;
    }

    private String createVertexId() {
        return TERM_MENTION_VERTEX_ID_PREFIX
                + "-"
                + this.propertyKey
                + "-"
                + this.start
                + "-"
                + this.end
                + "-"
                + this.process;
    }
}
