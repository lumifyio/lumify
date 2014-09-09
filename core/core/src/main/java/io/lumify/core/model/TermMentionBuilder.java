package io.lumify.core.model;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.VisibilityTranslator;
import org.json.JSONObject;
import org.securegraph.*;

public class TermMentionBuilder {
    private static final String TERM_MENTION_VERTEX_ID_PREFIX = "TM_";
    private Vertex sourceVertex;
    private final String propertyKey;
    private final int start;
    private final int end;
    private String title;
    private String conceptIri;
    private final JSONObject visibilitySource;
    private String process;
    private Vertex resolvedToVertex;

    public TermMentionBuilder(Vertex sourceVertex, String propertyKey, int start, int end, String title, String conceptIri, String visibilitySource) {
        this(sourceVertex, propertyKey, start, end, title, conceptIri, visibilitySourceToJson(visibilitySource));
    }

    private static JSONObject visibilitySourceToJson(String visibilitySource) {
        if (visibilitySource == null) {
            return new JSONObject();
        }
        if (visibilitySource.length() == 0) {
            return new JSONObject();
        }
        return new JSONObject(visibilitySource);
    }

    public TermMentionBuilder(Vertex sourceVertex, String propertyKey, int start, int end, String title, String conceptIri, JSONObject visibilitySource) {
        this.sourceVertex = sourceVertex;
        this.propertyKey = propertyKey;
        this.start = start;
        this.end = end;
        this.title = title;
        this.conceptIri = conceptIri;
        this.visibilitySource = visibilitySource;
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

    public TermMentionBuilder resolvedTo(Vertex resolvedToVertex) {
        this.resolvedToVertex = resolvedToVertex;
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
        String vertexId = createVertexId();
        Visibility visibility = visibilityTranslator.toVisibility(this.visibilitySource).getVisibility();
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
