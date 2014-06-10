package io.lumify.core.ingest.term.extraction;

import org.securegraph.Visibility;

public class VertexRelationship {
    private final TermMention source;
    private final Object targetId;
    private final String label;
    private final Visibility visibility;

    public VertexRelationship(TermMention source, Object targetId, String label, Visibility visibility) {
        this.source = source;
        this.targetId = targetId;
        this.label = label;
        this.visibility = visibility;
    }

    public TermMention getSource() {
        return source;
    }

    public Object getTargetId() {
        return targetId;
    }

    public String getLabel() {
        return label;
    }

    public Visibility getVisibility() {
        return visibility;
    }
}
