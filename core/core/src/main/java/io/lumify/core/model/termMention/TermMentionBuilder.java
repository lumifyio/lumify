package io.lumify.core.model.termMention;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.web.clientapi.model.VisibilityJson;
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
    private VisibilityJson visibilityJson;
    private String process;
    private Vertex resolvedToVertex;
    private Edge resolvedEdge;

    public TermMentionBuilder() {

    }

    /**
     * Copy an existing term mention.
     *
     * @param existingTermMention The term mention you would like to copy.
     * @param sourceVertex        The vertex that contains this term mention (ie Document, Html page, etc).
     */
    public TermMentionBuilder(Vertex existingTermMention, Vertex sourceVertex) {
        this.sourceVertex = sourceVertex;
        this.propertyKey = LumifyProperties.TERM_MENTION_PROPERTY_KEY.getPropertyValue(existingTermMention);
        this.start = LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(existingTermMention, 0);
        this.end = LumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(existingTermMention, 0);
        this.title = LumifyProperties.TERM_MENTION_TITLE.getPropertyValue(existingTermMention, "");
        this.conceptIri = LumifyProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyValue(existingTermMention, "");
        this.visibilityJson = LumifyProperties.TERM_MENTION_VISIBILITY_JSON.getPropertyValue(existingTermMention, "");
    }

    /**
     * The start offset within the property text that this term mention appears.
     */
    public TermMentionBuilder start(long start) {
        this.start = start;
        return this;
    }

    /**
     * The end offset within the property text that this term mention appears.
     */
    public TermMentionBuilder end(long end) {
        this.end = end;
        return this;
    }

    /**
     * The property key of the {@link io.lumify.core.model.properties.LumifyProperties#TEXT} that this term mention references.
     */
    public TermMentionBuilder propertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
        return this;
    }

    /**
     * Visibility JSON string. This will be applied to the newly created term.
     */
    public TermMentionBuilder visibilityJson(String visibilityJsonString) {
        return visibilityJson(visibilityJsonStringToJson(visibilityJsonString));
    }

    /**
     * Visibility JSON object. This will be applied to the newly created term.
     */
    public TermMentionBuilder visibilityJson(VisibilityJson visibilitySource) {
        this.visibilityJson = visibilitySource;
        return this;
    }

    private static VisibilityJson visibilityJsonStringToJson(String visibilityJsonString) {
        if (visibilityJsonString == null) {
            return new VisibilityJson();
        }
        if (visibilityJsonString.length() == 0) {
            return new VisibilityJson();
        }
        return ClientApiConverter.toClientApi(visibilityJsonString, VisibilityJson.class);
    }

    /**
     * If this is a resolved term mention. This allows setting that information.
     *
     * @param resolvedToVertex The vertex this term mention resolves to.
     * @param resolvedEdge     The edge that links the source vertex to the resolved vertex.
     */
    public TermMentionBuilder resolvedTo(Vertex resolvedToVertex, Edge resolvedEdge) {
        this.resolvedToVertex = resolvedToVertex;
        this.resolvedEdge = resolvedEdge;
        return this;
    }

    /**
     * The process that created this term mention.
     */
    public TermMentionBuilder process(String process) {
        this.process = process;
        return this;
    }

    /**
     * The vertex that contains this term mention (ie Document, Html page, etc).
     */
    public TermMentionBuilder sourceVertex(Vertex sourceVertex) {
        this.sourceVertex = sourceVertex;
        return this;
    }

    /**
     * The title/text of this term mention. (ie Job Ferner, Paris, etc).
     */
    public TermMentionBuilder title(String title) {
        this.title = title;
        return this;
    }

    /**
     * The concept type of this term mention.
     */
    public TermMentionBuilder conceptIri(String conceptIri) {
        this.conceptIri = conceptIri;
        return this;
    }

    /**
     * Saves the term mention to the graph.
     * <p/>
     * The resulting graph for non-resolved terms will be:
     * <p/>
     * Source  -- Has --> Term
     * Vertex             Mention
     * <p/>
     * The resulting graph for resolved terms will be:
     * <p/>
     * Source  -- Has --> Term    -- Resolved To --> Resolved
     * Vertex             Mention                    Vertex
     */
    public Vertex save(Graph graph, VisibilityTranslator visibilityTranslator, Authorizations authorizations) {
        checkNotNull(sourceVertex, "sourceVertex cannot be null");
        checkNotNull(propertyKey, "propertyKey cannot be null");
        checkNotNull(title, "title cannot be null");
        checkArgument(title.length() > 0, "title cannot be an empty string");
        checkNotNull(conceptIri, "conceptIri cannot be null");
        checkArgument(conceptIri.length() > 0, "conceptIri cannot be an empty string");
        checkNotNull(visibilityJson, "visibilityJson cannot be null");
        checkNotNull(process, "process cannot be null");
        checkArgument(process.length() > 0, "process cannot be an empty string");
        checkArgument(start >= 0, "start must be greater than or equal to 0");
        checkArgument(end >= 0, "start must be greater than or equal to 0");

        String vertexId = createVertexId();
        Visibility visibility = LumifyVisibility.and(visibilityTranslator.toVisibility(this.visibilityJson).getVisibility(), TermMentionRepository.VISIBILITY);
        VertexBuilder vertexBuilder = graph.prepareVertex(vertexId, visibility);
        LumifyProperties.TERM_MENTION_VISIBILITY_JSON.setProperty(vertexBuilder, this.visibilityJson, visibility);
        LumifyProperties.TERM_MENTION_CONCEPT_TYPE.setProperty(vertexBuilder, this.conceptIri, visibility);
        LumifyProperties.TERM_MENTION_TITLE.setProperty(vertexBuilder, this.title, visibility);
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
        Edge termMentionEdge = graph.addEdge(hasTermMentionId, this.sourceVertex, termMentionVertex, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, visibility, authorizations);
        LumifyProperties.TERM_MENTION_VISIBILITY_JSON.setProperty(termMentionEdge, this.visibilityJson, visibility, authorizations);
        if (this.resolvedToVertex != null) {
            String resolvedToId = vertexId + "_resolvedTo";
            Edge resolvedToEdge = graph.addEdge(resolvedToId, termMentionVertex, this.resolvedToVertex, LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, visibility, authorizations);
            LumifyProperties.TERM_MENTION_VISIBILITY_JSON.setProperty(resolvedToEdge, this.visibilityJson, visibility, authorizations);
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
